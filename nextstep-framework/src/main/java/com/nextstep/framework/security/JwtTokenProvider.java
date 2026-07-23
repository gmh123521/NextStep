package com.nextstep.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${nextstep.jwt.secret:nextstep-default-secret-key-change-me-please-32+}")
    private String secret;

    @Value("${nextstep.jwt.expire-millis:7200000}")
    private long expireMillis;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            log.debug("JWT parse failed: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(String token) {
        Claims c = parse(token);
        return c == null ? null : Long.valueOf(c.getSubject());
    }

    public long getExpireMillis() {
        return expireMillis;
    }
}
