package com.bartolomeo.clinics.booking.infrastructure.persistence.mapper;

import com.bartolomeo.clinics.booking.infrastructure.persistence.entity.AuthorityEntity;
import com.bartolomeo.clinics.booking.infrastructure.persistence.entity.UserEntity;
import com.bartolomeo.clinics.booking.user.domain.model.Role;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserEntity.builder()
                .id(user.getId())
                .login(user.getLogin())
                .passwordHash(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .langKey(user.getLangKey())
                .activated(user.isActivated())
                .authorities(toEntities(user.getRoles()))
                .build();
    }

    public User toDomainModel(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.builder()
                .id(entity.getId())
                .login(entity.getLogin())
                .passwordHash(entity.getPasswordHash())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .imageUrl(entity.getImageUrl())
                .langKey(entity.getLangKey())
                .activated(entity.isActivated())
                .roles(toRoles(entity.getAuthorities()))
                .build();
    }

    private java.util.Set<AuthorityEntity> toEntities(List<Role> roles) {
        java.util.Set<AuthorityEntity> entities = new java.util.HashSet<>();
        if (roles == null) {
            return entities;
        }
        roles.stream()
                .map(role -> new AuthorityEntity(role.name()))
                .forEach(entities::add);
        return entities;
    }

    private List<Role> toRoles(java.util.Set<AuthorityEntity> authorities) {
        if (authorities == null) {
            return List.of();
        }
        return authorities.stream()
                .map(AuthorityEntity::getName)
                .map(Role::valueOf)
                .toList();
    }
}
