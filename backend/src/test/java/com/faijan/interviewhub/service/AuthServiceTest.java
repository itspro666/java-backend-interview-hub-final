package com.faijan.interviewhub.service;

import com.faijan.interviewhub.dto.AuthRequest;
import com.faijan.interviewhub.dto.AuthResponse;
import com.faijan.interviewhub.entity.AppUser;
import com.faijan.interviewhub.entity.UserRole;
import com.faijan.interviewhub.repository.UserRepository;
import com.faijan.interviewhub.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void registrationAlwaysCreatesAUserRoleAndNormalizesEmail() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        JwtService jwt = mock(JwtService.class);
        when(users.existsByEmailIgnoreCase("learner@example.com")).thenReturn(false);
        when(passwords.encode("a-long-password")).thenReturn("bcrypt-hash");
        when(users.save(any(AppUser.class))).thenAnswer(call -> call.getArgument(0));
        when(jwt.issue(any(AppUser.class))).thenReturn(new JwtService.IssuedToken("signed-token", 21600, Instant.now().plusSeconds(21600)));

        AuthService service = new AuthService(users, passwords, jwt);
        AuthResponse response = service.register(new AuthRequest("Learner Name", " Learner@Example.com ", "a-long-password"));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertEquals("learner@example.com", saved.getValue().getEmail());
        assertEquals(UserRole.USER, saved.getValue().getRole());
        assertEquals("signed-token", response.accessToken());
    }

    @Test
    void duplicateRegistrationIsRejectedBeforePasswordHashing() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        JwtService jwt = mock(JwtService.class);
        when(users.existsByEmailIgnoreCase("learner@example.com")).thenReturn(true);

        AuthService service = new AuthService(users, passwords, jwt);
        assertThrows(ResponseStatusException.class, () -> service.register(new AuthRequest("Learner", "learner@example.com", "a-long-password")));
        verifyNoInteractions(passwords, jwt);
        verify(users, never()).save(any());
    }

    @Test
    void loginUsesTheSameGenericErrorForAnInvalidPassword() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        JwtService jwt = mock(JwtService.class);
        AppUser user = new AppUser("Learner", "learner@example.com", "bcrypt-hash", UserRole.USER);
        when(users.findByEmailIgnoreCase("learner@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        AuthService service = new AuthService(users, passwords, jwt);
        assertThrows(ResponseStatusException.class, () -> service.login(new com.faijan.interviewhub.dto.LoginRequest("learner@example.com", "wrong-password")));
        verifyNoInteractions(jwt);
    }
}
