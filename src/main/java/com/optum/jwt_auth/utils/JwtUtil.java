package com.optum.jwt_auth.utils;

import com.optum.jwt_auth.entities.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final String ACCESS_KEY = "THIS_IS_A_VERY_LONG_ACCESS_KEY_AT_LEAST_32_CHARS_LONG";
    private final String REFRESH_KEY = "THIS_IS_A_VERY_LONG_REFRESH_KEY_AT_LEAST_32_CHARS_LONG";

    public String generateToken(UserEntity user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(Keys.hmacShaKeyFor(ACCESS_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserEntity user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7))
                .signWith(Keys.hmacShaKeyFor(REFRESH_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(ACCESS_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractRefreshUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(REFRESH_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            extractUsername(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String refreshToken) {
        try {
            extractRefreshUsername(refreshToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getRefreshTokenValidity() {
        return 1000 * 60 * 60 * 24 * 7; // 7 days
    }
}

