package com.bartolomeo.clinics.booking.user.application.usecases;

import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.GetUserUseCase;
import com.bartolomeo.clinics.booking.user.domain.ports.out.UserRepository;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserUseCaseImpl implements GetUserUseCase {

    private final UserRepository repository;

    @Override
    public Optional<User> findByLogin(String login) {
        return repository.findByLogin(login);
    }
}
