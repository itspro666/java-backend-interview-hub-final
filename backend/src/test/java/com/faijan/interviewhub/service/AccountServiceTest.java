package com.faijan.interviewhub.service;

import com.faijan.interviewhub.dto.ChangePasswordRequest;
import com.faijan.interviewhub.entity.AppUser;
import com.faijan.interviewhub.entity.UserRole;
import com.faijan.interviewhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AccountServiceTest {
    @Test
    void changesPasswordOnlyAfterVerifyingCurrentPassword() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AppUser user = new AppUser("Learner", "learner@example.com", "old-hash", UserRole.USER);
        when(users.findByEmailIgnoreCase("learner@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("old-password", "old-hash")).thenReturn(true);
        when(encoder.matches("new-password", "old-hash")).thenReturn(false);
        when(encoder.encode("new-password")).thenReturn("new-hash");

        new AccountService(users, encoder).changePassword("learner@example.com", new ChangePasswordRequest("old-password", "new-password"));

        verify(encoder).encode("new-password");
        org.junit.jupiter.api.Assertions.assertEquals("new-hash", user.getPasswordHash());
    }

    @Test
    void rejectsAnIncorrectCurrentPassword() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AppUser user = new AppUser("Learner", "learner@example.com", "old-hash", UserRole.USER);
        when(users.findByEmailIgnoreCase("learner@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> new AccountService(users, encoder)
            .changePassword("learner@example.com", new ChangePasswordRequest("wrong", "new-password")));
        verify(encoder, never()).encode(anyString());
    }
}
