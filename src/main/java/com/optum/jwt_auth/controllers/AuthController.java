package com.optum.jwt_auth.controllers;

import com.optum.jwt_auth.DTO.request.AuthRequest;
import com.optum.jwt_auth.DTO.response.AuthResponse;
import com.optum.jwt_auth.entities.RefreshTokenEntity;
import com.optum.jwt_auth.entities.UserEntity;
import com.optum.jwt_auth.repo.RefreshTokenRepository;
import com.optum.jwt_auth.repo.UserRepository;
import com.optum.jwt_auth.services.security.AuthService;
import com.optum.jwt_auth.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    UserRepository userRepo;

    @Autowired
    RefreshTokenRepository refreshTokenRepo;

    @Autowired
    AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {

        Authentication authentication =  authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserEntity user = userRepo.findByUsername(request.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        Instant expiry = Instant.now().plusMillis(jwtUtil.getRefreshTokenValidity());
        refreshTokenRepo.save(
                RefreshTokenEntity.builder()
                        .token(refreshToken)
                        .user(user)
                        .expiry(expiry)
                        .build()
        );
        return ResponseEntity.ok(new AuthResponse(token, refreshToken));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || authHeader.isBlank()) {
            throw new RuntimeException("Missing refresh token");
        }

        // normalize header: remove "Bearer " prefix and surrounding quotes/whitespace
        String refreshToken = authHeader.trim();
        if (refreshToken.toLowerCase().startsWith("bearer ")) {
            refreshToken = refreshToken.substring(7).trim();
        }
        if ((refreshToken.startsWith("\"") && refreshToken.endsWith("\"")) ||
                (refreshToken.startsWith("'") && refreshToken.endsWith("'"))) {
            refreshToken = refreshToken.substring(1, refreshToken.length() - 1);
        }

        RefreshTokenEntity tokenEntity = refreshTokenRepo
                .findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        String newAccessToken = "";
        if (tokenEntity.getExpiry().isBefore(Instant.now())) {
            newAccessToken =
                    jwtUtil.generateToken(tokenEntity.getUser());
            refreshToken =
                    jwtUtil.generateRefreshToken(tokenEntity.getUser());
        Instant newExpiry = Instant.now().plusMillis(jwtUtil.getRefreshTokenValidity());
            tokenEntity.setToken(refreshToken);
            tokenEntity.setExpiry(newExpiry);
            refreshTokenRepo.save(tokenEntity);
        } else {
            newAccessToken =
                    jwtUtil.generateToken(tokenEntity.getUser());
        }

        return new AuthResponse(newAccessToken, refreshToken);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
//        if (userRepo.findByUsername(request.getUsername()).isPresent()) {
//            return ResponseEntity.badRequest().body("Username already exists");
//        }
        authService.registerUser(request.getUsername(), request.getPassword(), request.getRoles());
        return ResponseEntity.ok("User registered successfully");
    }

}
