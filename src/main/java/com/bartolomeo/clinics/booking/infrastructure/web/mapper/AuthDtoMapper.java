package com.bartolomeo.clinics.booking.infrastructure.web.mapper;

import com.bartolomeo.clinics.booking.infrastructure.web.dto.LoginRequest;
import com.bartolomeo.clinics.booking.infrastructure.web.dto.RegisterRequest;
import com.bartolomeo.clinics.booking.infrastructure.web.dto.SessionResponse;
import com.bartolomeo.clinics.booking.infrastructure.web.dto.UserResponse;
import com.bartolomeo.clinics.booking.user.domain.model.LoginCredentials;
import com.bartolomeo.clinics.booking.user.domain.model.RegistrationData;
import com.bartolomeo.clinics.booking.user.domain.model.Session;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import lombok.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class AuthDtoMapper {

    public RegistrationData toDomain(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        return RegistrationData.builder()
                .login(request.getLogin())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .langKey(request.getLangKey())
                .build();
    }

    public LoginCredentials toDomain(LoginRequest request) {
        if (request == null) {
            return null;
        }
        return LoginCredentials.builder()
                .login(request.getLogin())
                .password(request.getPassword())
                .build();
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .langKey(user.getLangKey())
                .activated(user.isActivated())
                .authorities(user.getRoles() == null
                        ? List.of()
                        : user.getRoles().stream().map(Enum::name).toList())
                .build();
    }

    public SessionResponse toResponse(Session session) {
        if (session == null) {
            return null;
        }
        long expiresIn = Duration.between(Instant.now(), session.getExpiresAt()).toSeconds();
        String scope = session.getUser().getRoles() == null
                ? ""
                : session.getUser().getRoles().stream().map(Enum::name)
                        .reduce((a, b) -> a + " " + b).orElse("");

        return SessionResponse.builder()
                .idToken(session.getToken())
                .accessToken(session.getToken())
                .tokenType("bearer")
                .expiresIn(Math.max(expiresIn, 0))
                .scope(scope)
                .user(toResponse(session.getUser()))
                .build();
    }
}
