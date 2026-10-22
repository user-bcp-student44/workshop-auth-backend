package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuthResourceTest {
    private static final String TEST_SECRET = "test-only-auth-token-secret-32-bytes";

    @Test
    void logsInWithWorkshopUser() throws Exception {
        Response response = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
            .when().post("/auth/login");

        response.then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("token", not(emptyOrNullString()));

        String token = response.jsonPath().getString("token");
        JsonNode claims = verifiedClaims(token);
        assertEquals("student", claims.path("sub").asText());
        assertEquals("angular-workshop", claims.path("client").asText());
        assertTrue(claims.path("iat").isIntegralNumber());
        assertTrue(claims.path("exp").isIntegralNumber());
        assertTrue(claims.path("exp").asLong() > claims.path("iat").asLong());
        long now = Instant.now().getEpochSecond();
        long lifetimeDifference = response.jsonPath().getLong("expiresIn")
            - (claims.path("exp").asLong() - now);
        assertTrue(Math.abs(lifetimeDifference) <= 1);
    }

    @Test
    void rejectsInvalidCredentialsWithoutIssuingToken() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"wrong\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("$", not(hasKey("token")));
    }

    @Test
    void authenticationLogsDoNotContainTokenOrCredentialValues() {
        Logger rootLogger = Logger.getLogger("");
        ByteArrayOutputStream capturedLogs = new ByteArrayOutputStream();
        Handler captureHandler = new StreamHandler(capturedLogs, new SimpleFormatter());
        captureHandler.setLevel(Level.ALL);
        rootLogger.addHandler(captureHandler);
        String token;

        try {
            Response successfulLogin = given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                    .when().post("/auth/login");
            successfulLogin.then().statusCode(200);
            token = successfulLogin.jsonPath().getString("token");
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"wrong\",\"client\":\"angular-workshop\"}")
                    .when().post("/auth/login")
                    .then().statusCode(401);
            assertRejectedLogin(validCredentials());
        } finally {
            captureHandler.flush();
            rootLogger.removeHandler(captureHandler);
            captureHandler.close();
        }

        String logOutput = capturedLogs.toString(StandardCharsets.UTF_8);
        assertFalse(logOutput.contains(TEST_SECRET));
        assertFalse(logOutput.contains("Workshop123!"));
        assertFalse(logOutput.contains("wrong"));
        assertFalse(logOutput.contains(token));
    }

    @Test
    void rejectsValidCredentialsWhenClientIsOmitted() {
        assertRejectedLogin(validCredentials());
    }

    @Test
    void rejectsValidCredentialsWhenClientIsNull() {
        Map<String, Object> request = validCredentials();
        request.put("client", null);

        assertRejectedLogin(request);
    }

    @Test
    void rejectsValidCredentialsWhenClientIsEmpty() {
        Map<String, Object> request = validCredentials();
        request.put("client", "");

        assertRejectedLogin(request);
    }

    @Test
    void rejectsValidCredentialsWhenClientIsWhitespace() {
        Map<String, Object> request = validCredentials();
        request.put("client", "   ");

        assertRejectedLogin(request);
    }

    private Map<String, Object> validCredentials() {
        Map<String, Object> request = new HashMap<>();
        request.put("username", "student");
        request.put("password", "Workshop123!");
        return request;
    }

    private void assertRejectedLogin(Map<String, Object> request) {
        given()
                .contentType("application/json")
                .body(request)
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("$", not(hasKey("token")));
    }

    private JsonNode verifiedClaims(String token) throws Exception {
        String[] segments = token.split("\\.");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode header = mapper.readTree(decode(segments[0]));
        JsonNode claims = mapper.readTree(decode(segments[1]));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expectedSignature = mac.doFinal(
                (segments[0] + "." + segments[1]).getBytes(StandardCharsets.US_ASCII));

        assertEquals(3, segments.length);
        assertEquals("HS256", header.path("alg").asText());
        assertArrayEquals(expectedSignature, decode(segments[2]));
        return claims;
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
