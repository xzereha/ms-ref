package com.microservices.user.config;

import com.microservices.common.security.JwtUtil;
import com.microservices.common.vault.AbstractVaultConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.PublicKey;

@Configuration
@ConditionalOnProperty(name = "vault.enabled", havingValue = "true", matchIfMissing = true)
public class VaultConfig extends AbstractVaultConfig {

    public VaultConfig(
            @Value("${vault.addr:http://vault:8200}") String vaultAddr,
            @Value("${vault.token:}") String vaultToken,
            @Value("${spring.application.name}") String serviceName) {
        super(vaultAddr, vaultToken, serviceName);
    }

    @Bean
    public JwtUtil jwtUtil() throws Exception {
        String token = resolveVaultToken();
        var keys = fetchJwtKeys(token);
        PublicKey publicKey = parsePublicKey(keys.get("public_key"));
        return new JwtUtil(publicKey);
    }
}
