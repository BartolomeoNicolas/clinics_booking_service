package com.bartolomeo.clinics.booking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "authority")
public class AuthorityEntity {

    @Id
    @Column(name = "name", length = 50)
    private String name;

    protected AuthorityEntity() {
    }

    public AuthorityEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
