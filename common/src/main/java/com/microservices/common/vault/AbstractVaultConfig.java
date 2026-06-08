package com.microservices.common.vault;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

public abstract class AbstractVaultConfig {

    protected final String vaultAddr;
    protected final String vaultToken;
    protected final String serviceName;

    protected AbstractVaultConfig(String vaultAddr, String vaultToken, String serviceName) {
        this.vaultAddr = vaultAddr;
        this.vaultToken = vaultToken;
        this.serviceName = serviceName;
    }

    protected String resolveVaultToken() {
        if (vaultToken != null && !vaultToken.isBlank()) {
            return vaultToken;
        }
        Path secretFile = Path.of("/run/secrets/vault-token-" + serviceName);
        if (Files.exists(secretFile)) {
            try {
                return Files.readString(secretFile).trim();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read Vault token from " + secretFile, e);
            }
        }
        throw new IllegalStateException(
                "VAULT_TOKEN not provided. Set VAULT_TOKEN env var or mount a Docker secret.");
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> fetchJwtKeys(String token) {
        RestTemplate rest = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var request = new HttpEntity<>(null, headers);

        String url = vaultAddr + "/v1/microservices/data/jwt";
        var response = rest.exchange(url, HttpMethod.GET, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null) throw new RuntimeException("Empty response from Vault");
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data == null) throw new RuntimeException("Vault response missing 'data'");
        Map<String, String> secretData = (Map<String, String>) data.get("data");
        if (secretData == null) throw new RuntimeException("Vault response missing 'data.data'");

        return secretData;
    }

    public static PublicKey parsePublicKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static PrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}
