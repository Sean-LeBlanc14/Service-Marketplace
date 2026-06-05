package com.ServiceMarketplace.service_marketplace;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ServiceMarketplace.service_marketplace.service.JwtService;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
        .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", SECRET);
        ReflectionTestUtils.setField(jwtService, "JWT_EXPIRATION", 60_000L);
    }

    @Test
    void generateToken_extractsClaimsAndValidatesToken() {
        String token = jwtService.generateToken("student@calpoly.edu");

        assertThat(jwtService.extractEmail(token)).isEqualTo("student@calpoly.edu");
        assertThat(jwtService.isTokenValid(token, "student@calpoly.edu")).isTrue();
        assertThat(jwtService.isTokenExpired(token)).isFalse();

        Date expiration = jwtService.extractClaim(token, claims -> claims.getExpiration());
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void isTokenValid_wrongEmail_returnsFalse() {
        String token = jwtService.generateToken("student@calpoly.edu");

        assertThat(jwtService.isTokenValid(token, "other@calpoly.edu")).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        JwtService spyJwtService = org.mockito.Mockito.spy(jwtService);
        org.mockito.Mockito.doReturn("student@calpoly.edu")
            .when(spyJwtService).extractEmail("expired-token");
        org.mockito.Mockito.doReturn(true)
            .when(spyJwtService).isTokenExpired("expired-token");

        assertThat(spyJwtService.isTokenValid("expired-token", "student@calpoly.edu")).isFalse();
    }
}
