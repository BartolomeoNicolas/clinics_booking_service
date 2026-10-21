package com.bartolomeo.clinics.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA del usuario.
 *
 * <p>Las anotaciones de persistencia viven acá y no en el modelo de dominio: el modelo no
 * depende de JPA.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login", nullable = false, length = 50, unique = true)
    private String login;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "lang_key", nullable = false, length = 10)
    private String langKey;

    @Column(name = "activated", nullable = false)
    private boolean activated;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_authority",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_name", referencedColumnName = "name"))
    private Set<AuthorityEntity> authorities = new HashSet<>();
}
