package com.sylvester.bankapp.user.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email on", columnList = "email")
})
public class User {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String username;
    @Column(unique = true)
    private String email;
    private String address;
    private String city;
    private String country;
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
    private Instant deletedAt;
    private Instant createdDate;
    private Instant modifiedDate;
    @PrePersist
    public void prePersist() {
        createdDate = Instant.now();
        deletedAt = null;
    }
    @PreUpdate
    public void preUpdate() {
        modifiedDate = Instant.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }


}
