#!/bin/sh
# vault/init.sh — Vault container entrypoint.
#
# Responsibilities (in order):
#   1. Start Vault server in background with the file-backed config
#   2. If Vault has NOT been initialized yet (first-ever run):
#       a. Initialise with a single unseal key (dev convenience — real
#          production splits this across N operators)
#       b. Unseal
#       c. Enable KV v2 secrets engine at microservices/
#       d. Generate an RS256 key pair and store it
#       e. Create one Vault policy per service (least privilege)
#       f. Issue a long-lived (720 h) token for each service
#       g. Write tokens to the shared volume so service containers can read them
#   3. If Vault IS already initialized (subsequent runs):
#       a. Unseal with the stored key — data survives from the previous run
#   4. Bring Vault to foreground
#
# Shared volume layout:
#   /vault/file/       — persistent Vault storage (survives restarts)
#   /vault/secrets/    — written by this script, mounted at /run/secrets/ on
#                        every service container so VaultConfig can find tokens
#   /vault/file/init.json  — init output (unseal key + root token), kept only
#                            for operational reference; service tokens are
#                            regenerated only when missing

set -e

# ── Paths ──────────────────────────────────────────────────────────────────
VAULT_CONFIG="/vault/config/vault.hcl"
VAULT_FILE="/vault/file"
SHARED_SECRETS="/vault/secrets"
INIT_FILE="$VAULT_FILE/init.json"
UNSEAL_KEY_FILE="$VAULT_FILE/unseal_key"

SERVICES="api-gateway user-service booking-service"

# ── Helpers ────────────────────────────────────────────────────────────────

log()   { echo "==> $*"; }
ok()    { echo "   ✓ $*"; }
fail()  { echo "   ✗ $*" >&2; exit 1; }

# ── Step functions ─────────────────────────────────────────────────────────

start_vault_server() {
    log "Starting Vault server in background"
    mkdir -p "$VAULT_FILE" "$SHARED_SECRETS"
    vault server -config="$VAULT_CONFIG" &
    VAULT_PID=$!
    sleep 2
    ok "Vault server PID $VAULT_PID"
}

install_tools() {
    log "Installing openssl and jq"
    apk add --no-cache openssl jq >/dev/null 2>&1
    ok "Tools installed"
}

initialize_vault() {
    log "Initialising Vault (single-key threshold)"
    vault operator init \
        -key-shares=1 \
        -key-threshold=1 \
        -format=json > "$INIT_FILE"
    ok "Init complete"
}

save_unseal_key() {
    UNSEAL_KEY=$(jq -r '.unseal_keys_b64[0]' "$INIT_FILE")
    echo "$UNSEAL_KEY" > "$UNSEAL_KEY_FILE"
    ok "Unseal key saved"
}

unseal_vault() {
    log "Unsealing Vault"
    UNSEAL_KEY=$(cat "$UNSEAL_KEY_FILE")
    vault operator unseal "$UNSEAL_KEY"
    ok "Vault unsealed"
}

login_with_root_token() {
    log "Authenticating with root token"
    ROOT_TOKEN=$(jq -r '.root_token' "$INIT_FILE")
    vault login "$ROOT_TOKEN"
    ok "Authenticated"
}

enable_kv_engine() {
    log "Enabling KV v2 at microservices/"
    vault secrets enable -path=microservices kv-v2 2>/dev/null || true
    ok "KV v2 engine ready"
}

generate_rsa_keys() {
    log "Generating 2048-bit RSA key pair"
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
        -out /tmp/private.pem 2>/dev/null
    openssl pkey -in /tmp/private.pem -pubout \
        -out /tmp/public.pem 2>/dev/null
    ok "RSA key pair generated"

    log "Storing keys in Vault at microservices/jwt"
    vault kv put microservices/jwt \
        private_key=@/tmp/private.pem \
        public_key=@/tmp/public.pem
    rm -f /tmp/private.pem /tmp/public.pem
    ok "Keys stored in Vault"
}

create_service_policies() {
    log "Creating one policy per service (read-only on microservices/jwt)"
    for svc in $SERVICES; do
        vault policy write "$svc" - <<POLICY 2>/dev/null || true
path "microservices/data/jwt" {
  capabilities = ["read"]
}
POLICY
        ok "Policy '$svc' created"
    done
}

issue_service_tokens() {
    log "Issuing 720-hour tokens for all services"
    for svc in $SERVICES; do
        vault token create \
            -policy="$svc" \
            -ttl=720h \
            -field=token > "$SHARED_SECRETS/vault-token-$svc"
        ok "Token written to vault-token-$svc"
    done
}

ensure_service_tokens() {
    # If the shared volume was wiped (e.g. `docker compose down -v`) the
    # tokens are gone even though Vault's persistent storage still has its
    # init data. Re-create tokens using the stored root token.
    if [ ! -f "$SHARED_SECRETS/vault-token-api-gateway" ]; then
        log "Service tokens missing — reissuing"
        apk add --no-cache jq >/dev/null 2>&1
        ROOT_TOKEN=$(jq -r '.root_token' "$INIT_FILE")
        vault login "$ROOT_TOKEN"
        issue_service_tokens
        ok "Tokens refreshed"
    fi
}

wait_for_vault() {
    log "Bringing Vault to foreground"
    wait $VAULT_PID
}

# ── Main ───────────────────────────────────────────────────────────────────

start_vault_server

if [ ! -f "$INIT_FILE" ]; then
    echo ""
    echo "═══════════════════════════════════════════"
    echo "  First run — full Vault bootstrap"
    echo "═══════════════════════════════════════════"
    echo ""

    install_tools
    initialize_vault
    save_unseal_key
    unseal_vault
    login_with_root_token
    enable_kv_engine
    generate_rsa_keys
    create_service_policies
    issue_service_tokens

    echo ""
    echo "═══════════════════════════════════════════"
    echo "  Vault initialised and bootstrapped"
    echo "═══════════════════════════════════════════"
    echo ""
else
    echo ""
    echo "═══════════════════════════════════════════"
    echo "  Vault already initialised — unsealing"
    echo "═══════════════════════════════════════════"
    echo ""
    unseal_vault
fi

ensure_service_tokens
wait_for_vault
