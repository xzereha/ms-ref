package com.microservices.gateway.config;

import com.microservices.common.security.JwtUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPairGenerator;

// Local JWT configuration — used when Vault is unavailable (development / tests).
// Generates an ephemeral RSA key pair at startup.
// This is intentionally kept simple — in production, VaultConfig is used.
@Configuration
@ConditionalOnProperty(name = "vault.enabled", havingValue = "false")
public class LocalJwtConfig {

    @Bean
    public JwtUtil jwtUtil() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        var pair = gen.generateKeyPair();
        return new JwtUtil(pair.getPublic(), pair.getPrivate());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
