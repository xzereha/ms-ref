package com.microservices.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

// JwtUtil — shared utility for RS256-signed JWT tokens.
//
// Two usage modes:
//   1. SIGN + VERIFY  — api-gateway has the PrivateKey and creates tokens
//   2. VERIFY only    — internal services have only the PublicKey for validation
//
// The keys are fetched from HashiCorp Vault at startup (see VaultConfig in each
// service) rather than being embedded in configuration files. This means:
//   - Key rotation can happen centrally in Vault
//   - No private key material ever lives in a git repo or config file
//   - Services can be redeployed without changing code when keys rotate
//
// In production you could also store per-service database credentials,
// API keys, and TLS certificates in Vault using the same pattern.

public class JwtUtil {

    private final PublicKey publicKey;
    private final PrivateKey privateKey;   // null when only verification is needed
    private final long expirationMs;       // token lifetime in milliseconds

    public JwtUtil(PublicKey publicKey, PrivateKey privateKey, long expirationMs) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.expirationMs = expirationMs;
    }

    public JwtUtil(PublicKey publicKey, PrivateKey privateKey) {
        this(publicKey, privateKey, 3_600_000L); // default 1 hour
    }

    public JwtUtil(PublicKey publicKey) {
        this(publicKey, null, 3_600_000L);
    }

    // ── Token creation ─────────────────────────────────────────────────────
    // Called by the api-gateway after a successful login.
    // Embeds both the user's email (as "sub") and UUID (as "user_id" claim).
    public String generateToken(String email, UUID userId) {
        if (privateKey == null) {
            throw new IllegalStateException("Private key not available — cannot sign tokens");
        }
        Date now = new Date();
        return Jwts.builder()
            .subject(email)
            .claim("user_id", userId.toString())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact();
    }

    // ── Token validation ───────────────────────────────────────────────────
    // Verifies the JWT signature using the public key and returns the claims.
    // Throws on invalid signature / expired token.
    public Claims validateToken(String token) {
        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    // ── Convenience: extract the email (subject) from a validated token ────
    public String getEmailFromToken(String token) {
        return validateToken(token).getSubject();
    }
}
