package com.wealthwise.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void generateTokenContainsUserIdClaim() {
        JwtService jwtService = new JwtService(
            "wealthwise-default-jwt-secret-key-change-in-production-1234567890",
            60000
        );

        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(userId, jwtService.extractUserId(token));
    }
}

