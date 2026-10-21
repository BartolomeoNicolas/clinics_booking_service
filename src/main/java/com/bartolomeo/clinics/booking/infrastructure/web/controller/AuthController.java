package com.bartolomeo.clinics.booking.infrastructure.web.controller;

import com.bartolomeo.clinics.booking.infrastructure.web.dto.LoginRequest;
import com.bartolomeo.clinics.booking.infrastructure.web.dto.RegisterRequest;
import com.bartolomeo.clinics.booking.infrastructure.web.dto.SessionResponse;
import com.bartolomeo.clinics.booking.infrastructure.web.dto.UserResponse;
import com.bartolomeo.clinics.booking.infrastructure.web.mapper.AuthDtoMapper;
import com.bartolomeo.clinics.booking.user.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de registro, inicio de sesion y perfil.
 *
 * <p>El controller inyecta la <strong>fachada</strong>, no los casos de uso: la fachada es la
 * que resuelve la ausencia en excepciones de dominio. Y no traduce excepciones a HTTP: lo
 * hace el {@code GlobalExceptionHandler}, que produce {@code problem+json} con un
 * {@code code} estable.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthDtoMapper authDtoMapper;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authDtoMapper.toResponse(
                authService.register(authDtoMapper.toDomain(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                authDtoMapper.toResponse(authService.login(authDtoMapper.toDomain(request))));
    }
}
