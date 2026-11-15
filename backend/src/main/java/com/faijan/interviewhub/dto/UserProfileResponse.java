package com.faijan.interviewhub.dto;
import java.time.Instant;
public record UserProfileResponse(Long id,String fullName,String email,String role,Instant createdAt) {}
