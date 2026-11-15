package com.faijan.interviewhub.dto;
import jakarta.validation.constraints.*;
public record AuthRequest(@NotBlank @Size(max=120) String fullName,@NotBlank @Email @Size(max=190) String email,@NotBlank @Size(min=8,max=72) String password) {}
