# Vault server configuration — production-style file-backed storage.
#
# In production you would:
#   - Use the raft storage backend with a 3-or-5-node HA cluster
#   - Enable TLS on the listener (Vault PKI can issue the cert)
#   - Set a proper api_addr pointing to the load balancer / DNS
#   - Distribute unseal keys to separate operators (never automate)

storage "file" {
  path = "/vault/file"
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = true   # enable TLS in production
}

# mlock is disabled because the syscall is often unavailable inside Docker
# containers. The IPC_LOCK capability should grant it, but this varies across
# host configurations. Production deployments should enable mlock instead.
disable_mlock = true

api_addr     = "http://0.0.0.0:8200"
cluster_addr = "http://0.0.0.0:8201"
