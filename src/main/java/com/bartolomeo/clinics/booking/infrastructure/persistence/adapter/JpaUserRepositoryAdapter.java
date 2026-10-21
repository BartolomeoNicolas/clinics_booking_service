package com.bartolomeo.clinics.booking.infrastructure.persistence.adapter;

import com.bartolomeo.clinics.booking.infrastructure.persistence.entity.UserEntity;
import com.bartolomeo.clinics.booking.infrastructure.persistence.mapper.UserMapper;
import com.bartolomeo.clinics.booking.infrastructure.persistence.repository.JpaUserRepository;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.out.UserRepository;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementa el port de salida de usuarios sobre Spring Data.
 *
 * <p>Es el unico lugar del servicio que conoce a la vez la interfaz de Spring Data y el port
 * del dominio. El adaptador es un {@code @Component}, no un {@code @Repository}.
 */
@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findByLogin(String login) {
        return jpaUserRepository.findByLogin(login)
                .map(userMapper::toDomainModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(userMapper::toDomainModel);
    }

    @Override
    public boolean existsByLogin(String login) {
        return jpaUserRepository.existsByLogin(login);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        return userMapper.toDomainModel(jpaUserRepository.save(entity));
    }
}
