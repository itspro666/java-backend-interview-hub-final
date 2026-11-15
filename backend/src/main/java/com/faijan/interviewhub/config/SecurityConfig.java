package com.faijan.interviewhub.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {
    @Bean SecretKey jwtSecretKey(@Value("${app.security.jwt.secret}") String base64Secret){
        byte[] key=Base64.getDecoder().decode(base64Secret);
        if(key.length<32) throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
        return new SecretKeySpec(key,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key){return new NimbusJwtEncoder(new ImmutableSecret<>(key));}
    @Bean JwtDecoder jwtDecoder(SecretKey key){NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer("interview-hub"));return decoder;}
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable).cors(Customizer.withDefaults()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers("/api/auth/**","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html","/actuator/health","/actuator/health/**","/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/questions","/api/questions/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/questions","/api/questions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,"/api/questions","/api/questions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,"/api/questions","/api/questions/**").hasRole("ADMIN")
                .requestMatchers("/api/bookmarks/**","/api/practice/**","/api/analytics/**","/api/users/**").authenticated()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(jwt->{
                List<String> roles=jwt.getClaimAsStringList("roles");
                var authorities=(roles==null?List.<String>of():roles).stream().map(r->new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+r)).collect(Collectors.toList());
                return new JwtAuthenticationToken(jwt,authorities,jwt.getSubject());
            })))
            .build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-origin:http://localhost:5173}") String origin){
        CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin,"http://localhost:4173","http://localhost:3000"));c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type","Accept"));c.setExposedHeaders(List.of("Authorization"));c.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;
    }
}
