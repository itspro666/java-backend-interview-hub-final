package com.faijan.interviewhub.controller;
import com.faijan.interviewhub.dto.*;import com.faijan.interviewhub.service.AccountService;import jakarta.validation.Valid;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/users") public class AccountController {
 private final AccountService service;public AccountController(AccountService service){this.service=service;}
 @GetMapping("/me") public UserProfileResponse profile(Authentication auth){return service.profile(auth.getName());}
 @PutMapping("/me/password") public void changePassword(Authentication auth,@Valid @RequestBody ChangePasswordRequest request){service.changePassword(auth.getName(),request);}
}
