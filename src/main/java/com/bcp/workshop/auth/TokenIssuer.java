package com.bcp.workshop.auth;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@ApplicationScoped
public class TokenIssuer {
    static final long TOKEN_LIFETIME_SECONDS = 3600;
    private static final String SECRET_CONFIGURATION_ERROR =
            "AUTH_TOKEN_SECRET must contain at least 32 UTF-8 bytes";

    private final SecretKey signingKey;
    private final Clock clock;

    @Inject
    public TokenIssuer(@ConfigProperty(name = "auth.token.secret") String secret) {
        this(secret, Clock.systemUTC());
    }

    TokenIssuer(String secret, Clock clock) {
        if (secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(SECRET_CONFIGURATION_ERROR);
        }

        this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.clock = clock;
    }

    IssuedToken issue(String username, String client) {
        long issuedAt = clock.instant().getEpochSecond();
        long expiresAt = issuedAt + TOKEN_LIFETIME_SECONDS;
        String value = Jwt.claims()
                .subject(username)
                .claim("client", client)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .jws()
                .algorithm(SignatureAlgorithm.HS256)
                .sign(signingKey);

        return new IssuedToken(value, issuedAt, expiresAt);
    }

    long expiresInSeconds(IssuedToken issuedToken) {
        return Math.max(0, issuedToken.expiresAt() - clock.instant().getEpochSecond());
    }

    record IssuedToken(String value, long issuedAt, long expiresAt) {}
}