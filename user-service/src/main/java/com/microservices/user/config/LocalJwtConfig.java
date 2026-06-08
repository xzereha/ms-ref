package com.microservices.user.config;

import com.microservices.common.security.JwtUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPairGenerator;

@Configuration
@ConditionalOnProperty(name = "vault.enabled", havingValue = "false")
public class LocalJwtConfig {

    @Bean
    public JwtUtil jwtUtil() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        var pair = gen.generateKeyPair();
        return new JwtUtil(pair.getPublic());
    }
}
