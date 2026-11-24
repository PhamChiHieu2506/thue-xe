package com.example.auth_service.security;
import com.example.auth_service.account.AccountRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.util.List;

@Configuration
public class AuthSecurity {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean UserDetailsService users(AccountRepository accounts) {
        return username -> accounts.findByUsernameIgnoreCase(username)
            .map(a->User.withUsername(a.getUsername()).password(a.getPasswordHash())
                .roles(a.getRole()).disabled(!a.isEnabled()).build())
            .orElseThrow(()->new UsernameNotFoundException("Tai khoan khong hop le"));
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users,PasswordEncoder encoder) {
        var provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder(java.security.interfaces.RSAPublicKey publicKey,
            @org.springframework.beans.factory.annotation.Value("${app.jwt.issuer}") String issuer) {
        var decoder=org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withPublicKey(publicKey).build();
        org.springframework.security.oauth2.core.OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> audience=
                new org.springframework.security.oauth2.jwt.JwtClaimValidator<java.util.List<String>>("aud",v->v!=null&&v.contains("auth-api"));
        org.springframework.security.oauth2.core.OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> required=jwt->
                jwt.getExpiresAt()!=null&&jwt.getSubject()!=null&&!jwt.getSubject().isBlank()
                ?org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                :org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(issuer),audience,required));
        return decoder;
    }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(c->c.disable()).cors(c->{})
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers("/api/auth/login","/api/auth/register","/actuator/health","/actuator/health/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/auth/me").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT,"/api/auth/me").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT,"/api/auth/password").authenticated()
                .requestMatchers("/api/admin/accounts","/api/admin/accounts/**").authenticated()
                .anyRequest().denyAll())
            .oauth2ResourceServer(o->o.jwt(j->{}))
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{
                res.setStatus(401);res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Can dang nhap\"}");
            }).accessDeniedHandler((req,res,ex)->{
                res.setStatus(403);res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Khong co quyen truy cap\"}");
            })).build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource() {
        var c=new CorsConfiguration();
        c.setAllowedOrigins(List.of("http://localhost:8080","http://127.0.0.1:8080","http://localhost:8090","http://127.0.0.1:8090"));
        c.setAllowedMethods(List.of("GET","PUT","POST","OPTIONS"));c.setAllowedHeaders(List.of("Content-Type","Authorization"));
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/auth/**",c);source.registerCorsConfiguration("/api/admin/**",c);return source;
    }
}
