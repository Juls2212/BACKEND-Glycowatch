package com.glycowatch.backend.security;

import com.glycowatch.backend.auth.model.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    public String generateAccessToken(UserEntity user) {
        return buildToken(user.getEmail(), TokenType.ACCESS, jwtProperties.accessTokenExpirationMinutes(), ChronoUnit.MINUTES);
    }

    public String generateRefreshToken(UserEntity user) {
        return buildToken(user.getEmail(), TokenType.REFRESH, jwtProperties.refreshTokenExpirationDays(), ChronoUnit.DAYS);
    }

    public boolean isTokenValid(String token, TokenType expectedType) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return expectedType.name().equals(tokenType);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(String subject, TokenType tokenType, long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant expiration = now.plus(amount, unit);

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(subject)
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}

