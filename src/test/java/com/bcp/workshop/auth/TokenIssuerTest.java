package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenIssuerTest {
    private static final String TEST_SECRET = "test-only-auth-token-secret-32-bytes";
    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

    @Test
    void issuesHs256TokenWithIdentityClientAndEpochTimes() throws Exception {
        TokenIssuer issuer = new TokenIssuer(TEST_SECRET,
                Clock.fixed(NOW, ZoneOffset.UTC));

        TokenIssuer.IssuedToken issuedToken = issuer.issue("student", "angular-workshop");
        String[] segments = issuedToken.value().split("\\.");
        JsonNode header = new ObjectMapper().readTree(decode(segments[0]));
        JsonNode claims = new ObjectMapper().readTree(decode(segments[1]));

        assertEquals(3, segments.length);
        assertEquals("HS256", header.path("alg").asText());
        assertArrayEquals(sign(segments[0] + "." + segments[1]), decodeBytes(segments[2]));
        assertEquals("student", claims.path("sub").asText());
        assertEquals("angular-workshop", claims.path("client").asText());
        assertTrue(claims.path("iat").isIntegralNumber());
        assertTrue(claims.path("exp").isIntegralNumber());
        assertEquals(NOW.getEpochSecond(), claims.path("iat").asLong());
        assertEquals(NOW.getEpochSecond() + TokenIssuer.TOKEN_LIFETIME_SECONDS,
                claims.path("exp").asLong());
        assertEquals(claims.path("iat").asLong(), issuedToken.issuedAt());
        assertEquals(claims.path("exp").asLong(), issuedToken.expiresAt());
        assertTrue(issuedToken.expiresAt() > issuedToken.issuedAt());
        assertEquals(TokenIssuer.TOKEN_LIFETIME_SECONDS, issuer.expiresInSeconds(issuedToken));
        assertFalse(new String(decode(segments[1]), StandardCharsets.UTF_8).contains(TEST_SECRET));
    }

    @Test
    void rejectsMissingEmptyOrTooShortSigningSecrets() {
        assertThrows(IllegalArgumentException.class, () -> new TokenIssuer(null, Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class, () -> new TokenIssuer("   ", Clock.systemUTC()));
        String shortSecret = "short-test-secret";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new TokenIssuer(shortSecret, Clock.systemUTC()));
        assertFalse(exception.getMessage().contains(shortSecret));
    }

    @Test
    void invalidSigningSecretIsNotWrittenToLogs() {
        Logger rootLogger = Logger.getLogger("");
        ByteArrayOutputStream capturedLogs = new ByteArrayOutputStream();
        String invalidSecret = "short-test-secret";
        Handler captureHandler = new StreamHandler(capturedLogs, new SimpleFormatter());
        captureHandler.setLevel(Level.ALL);
        rootLogger.addHandler(captureHandler);

        try {
            assertThrows(IllegalArgumentException.class,
                    () -> new TokenIssuer(invalidSecret, Clock.systemUTC()));
        } finally {
            captureHandler.flush();
            rootLogger.removeHandler(captureHandler);
            captureHandler.close();
        }

        assertFalse(capturedLogs.toString(StandardCharsets.UTF_8).contains(invalidSecret));
    }

    @Test
    void rejectsAlteredSignaturesWrongKeysAndExpiredTokens() throws Exception {
        TokenIssuer issuer = new TokenIssuer(TEST_SECRET,
                Clock.fixed(NOW, ZoneOffset.UTC));
        String token = issuer.issue("student", "angular-workshop").value();
        long expiresAt = NOW.getEpochSecond() + TokenIssuer.TOKEN_LIFETIME_SECONDS;

        assertTrue(isValidAt(token, TEST_SECRET, NOW.getEpochSecond()));
        assertTrue(isValidAt(token, TEST_SECRET, expiresAt - 1));
        assertFalse(isValidAt(token, TEST_SECRET, expiresAt));
        assertFalse(isValidAt(token, TEST_SECRET, expiresAt + 1));
        assertFalse(isValidAt(token, "different-test-secret-32-bytes-long", NOW.getEpochSecond()));
        assertFalse(isValidAt(alterSignature(token), TEST_SECRET, NOW.getEpochSecond()));
    }

    private boolean isValidAt(String token, String secret, long epochSecond) throws Exception {
        String[] segments = token.split("\\.");
        JsonNode claims = new ObjectMapper().readTree(decode(segments[1]));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expectedSignature = mac.doFinal(
                (segments[0] + "." + segments[1]).getBytes(StandardCharsets.US_ASCII));

        return MessageDigest.isEqual(expectedSignature, decode(segments[2]))
                && claims.path("exp").asLong() > epochSecond;
    }

    private String alterSignature(String token) {
        String[] segments = token.split("\\.");
        byte[] signature = decode(segments[2]);
        signature[0] ^= 1;
        return segments[0] + "." + segments[1] + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private byte[] sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] decode(String value) {
        return decodeBytes(value);
    }

    private byte[] decodeBytes(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}