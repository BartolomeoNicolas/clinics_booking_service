package com.bartolomeo.clinics.booking.infrastructure.web.controller;

import java.security.Principal;

import com.bartolomeo.clinics.booking.infrastructure.web.dto.UserResponse;
import com.bartolomeo.clinics.booking.infrastructure.web.mapper.AuthDtoMapper;
import com.bartolomeo.clinics.booking.user.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Usuario de la sesion actual.
 *
 * <p>La identidad sale del token verificado, no de un parametro: el cliente no puede
 * pedir el perfil de otro.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final AuthService authService;
    private final AuthDtoMapper authDtoMapper;

    @GetMapping
    public ResponseEntity<UserResponse> me(Principal principal) {
        return ResponseEntity.ok(authDtoMapper.toResponse(authService.getByLogin(principal.getName())));
    }
}
