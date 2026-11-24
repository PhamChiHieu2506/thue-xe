package com.example.auth_service.api;
import java.time.Instant;
import java.util.*;
import com.example.auth_service.account.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authentication;private final AccountRepository accounts;private final JwtEncoder encoder;
    private final String issuer;private final long ttl;private final ProfileService profiles;private final AccountService accountService;
    public AuthController(AuthenticationManager authentication,AccountRepository accounts,JwtEncoder encoder,
            @Value("${app.jwt.issuer}") String issuer,@Value("${app.jwt.ttl-seconds:900}") long ttl,ProfileService profiles,AccountService accountService) {
        this.authentication=authentication;this.accounts=accounts;this.encoder=encoder;this.issuer=issuer;this.ttl=ttl;this.profiles=profiles;this.accountService=accountService;
    }
    @GetMapping("/me")
    public ResponseEntity<ProfileView> me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(profiles.view(jwt.getSubject()));
    }
    @PutMapping("/me")
    public ResponseEntity<ProfileView> updateMe(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
                                              @RequestBody ProfileService.Update change) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(profiles.update(jwt.getSubject(),change));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<?> invalidJson() {
        return ResponseEntity.badRequest().body(Map.of("message","JSON khong hop le"));
    }
    @PostMapping("/register")
    public ResponseEntity<ProfileView> register(@RequestBody AccountService.Registration request){return ResponseEntity.status(201).header("Cache-Control","no-store").body(accountService.register(request));}
    public record PasswordChange(String currentPassword,String newPassword){}
    @PutMapping("/password")
    public Map<String,String> password(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,@RequestBody PasswordChange change){accountService.changePassword(jwt.getSubject(),change.currentPassword(),change.newPassword());return Map.of("message","Đã đổi mật khẩu. Hãy đăng nhập lại.");}
    public record LoginRequest(String username,String password) {}
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if(request.username()==null||request.username().isBlank()||request.username().length()>60
                ||request.password()==null||request.password().isBlank()||request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) {
            return ResponseEntity.badRequest().body(Map.of("message","Can nhap username va password hop le"));
        }
        try {
            var authenticated=authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username().trim(),request.password()));
            var account=accounts.findByUsernameIgnoreCase(authenticated.getName()).orElseThrow();
            Instant now=Instant.now();
            var claims=JwtClaimsSet.builder().issuer(issuer).subject(account.getId().toString())
                .audience(List.of("auth-api","carmanage-api","rental-api")).issuedAt(now).expiresAt(now.plusSeconds(ttl))
                .id(UUID.randomUUID().toString()).claim("upn",account.getUsername())
                .claim("groups",List.of(account.getRole()))
                .claim("permissions",RolePermissions.forRole(account.getRole())).build();
            String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),claims)).getTokenValue();
            return ResponseEntity.ok().header("Cache-Control","no-store").header("Pragma","no-cache")
                .body(Map.of("accessToken",token,"tokenType","Bearer","expiresIn",ttl,
                    "user",ProfileView.of(account)));
        } catch(AuthenticationException ex) {
            return ResponseEntity.status(401).body(Map.of("message","Ten dang nhap hoac mat khau khong dung"));
        }
    }
}
