package com.faijan.interviewhub.security;

import com.faijan.interviewhub.entity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final long ttlSeconds;
    public JwtService(JwtEncoder encoder,@Value("${app.security.jwt.ttl-seconds:21600}") long ttlSeconds){this.encoder=encoder;this.ttlSeconds=ttlSeconds;}
    public IssuedToken issue(AppUser user){
        Instant now=Instant.now(), expires=now.plusSeconds(ttlSeconds);
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer("interview-hub").issuedAt(now).expiresAt(expires).subject(user.getEmail()).claim("name",user.getFullName()).claim("roles",List.of(user.getRole().name())).build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new IssuedToken(token,ttlSeconds,expires);
    }
    public record IssuedToken(String value,long expiresIn,Instant expiresAt){}
}
