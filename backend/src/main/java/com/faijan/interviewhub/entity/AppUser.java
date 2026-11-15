package com.faijan.interviewhub.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(name = "uk_app_user_email", columnNames = "email"))
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String fullName;
    @Column(nullable = false, length = 190) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(name = "user_role", nullable = false, length = 20) private UserRole role = UserRole.USER;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private boolean enabled = true;
    protected AppUser() {}
    public AppUser(String fullName, String email, String passwordHash, UserRole role) { this.fullName=fullName; this.email=email; this.passwordHash=passwordHash; this.role=role; }
    public Long getId(){return id;} public String getFullName(){return fullName;} public String getEmail(){return email;}
    @JsonIgnore public String getPasswordHash(){return passwordHash;} public UserRole getRole(){return role;} public Instant getCreatedAt(){return createdAt;} public boolean isEnabled(){return enabled;}
    public void changePassword(String encodedPassword){this.passwordHash=encodedPassword;}
}
