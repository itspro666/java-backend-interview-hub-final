package com.faijan.interviewhub.controller;
import com.faijan.interviewhub.dto.*;import com.faijan.interviewhub.service.AuthService;
import jakarta.validation.Valid;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthService service;public AuthController(AuthService service){this.service=service;}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public AuthResponse register(@Valid @RequestBody AuthRequest request){return service.register(request);}
 @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request){return service.login(request);}
}
