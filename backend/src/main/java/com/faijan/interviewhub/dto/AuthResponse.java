package com.faijan.interviewhub.dto;
import java.time.Instant;
public record AuthResponse(String accessToken,String tokenType,long expiresIn,Instant expiresAt,UserProfile user) {
 public record UserProfile(Long id,String fullName,String email,String role) {}
}
