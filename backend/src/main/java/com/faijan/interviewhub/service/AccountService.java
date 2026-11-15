package com.faijan.interviewhub.service;
import com.faijan.interviewhub.dto.*;import com.faijan.interviewhub.entity.AppUser;import com.faijan.interviewhub.repository.UserRepository;import org.springframework.http.HttpStatus;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;
@Service public class AccountService {
 private final UserRepository users;private final PasswordEncoder encoder;
 public AccountService(UserRepository users,PasswordEncoder encoder){this.users=users;this.encoder=encoder;}
 @Transactional(readOnly=true) public UserProfileResponse profile(String email){AppUser u=find(email);return new UserProfileResponse(u.getId(),u.getFullName(),u.getEmail(),u.getRole().name(),u.getCreatedAt());}
 @Transactional public void changePassword(String email,ChangePasswordRequest request){AppUser u=find(email);if(!encoder.matches(request.currentPassword(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password is incorrect");if(encoder.matches(request.newPassword(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a different password");u.changePassword(encoder.encode(request.newPassword()));}
 private AppUser find(String email){return users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found"));}
}
