package com.studyos.service;

import com.studyos.dto.AuthDtos.*;
import com.studyos.model.User;
import com.studyos.repository.UserRepository;
import com.studyos.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        User u = User.builder()
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .name(req.name())
                .build();
        u = users.save(u);
        return new AuthResponse(jwt.generateToken(u.getId(), u.getEmail()), u.getId(), u.getEmail(), u.getName());
    }

    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new AuthResponse(jwt.generateToken(u.getId(), u.getEmail()), u.getId(), u.getEmail(), u.getName());
    }
}
