package com.faijan.interviewhub.service;

import com.faijan.interviewhub.dto.*;
import com.faijan.interviewhub.entity.*;
import com.faijan.interviewhub.repository.UserRepository;
import com.faijan.interviewhub.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;private final PasswordEncoder passwords;private final JwtService jwt;
    public AuthService(UserRepository users,PasswordEncoder passwords,JwtService jwt){this.users=users;this.passwords=passwords;this.jwt=jwt;}
    @Transactional
    public AuthResponse register(AuthRequest request){
        String email=request.email().trim().toLowerCase();
        if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");
        AppUser user=users.save(new AppUser(request.fullName().trim(),email,passwords.encode(request.password()),UserRole.USER));
        return response(user);
    }
    @Transactional(readOnly=true)
    public AuthResponse login(LoginRequest request){
        AppUser user=users.findByEmailIgnoreCase(request.email().trim()).filter(AppUser::isEnabled).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password"));
        if(!passwords.matches(request.password(),user.getPasswordHash()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password");
        return response(user);
    }
    private AuthResponse response(AppUser user){var token=jwt.issue(user);return new AuthResponse(token.value(),"Bearer",token.expiresIn(),token.expiresAt(),new AuthResponse.UserProfile(user.getId(),user.getFullName(),user.getEmail(),user.getRole().name()));}
}
