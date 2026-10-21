package com.bartolomeo.clinics.booking.infrastructure.persistence.repository;

import com.bartolomeo.clinics.booking.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLogin(String login);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
