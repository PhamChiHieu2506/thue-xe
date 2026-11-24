package com.example.rental_service.security;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration @EnableMethodSecurity
public class ApiSecurity {
    @Bean JwtDecoder jwtDecoder(@Value("${app.jwt.public-key-location}") Resource resource,
                               @Value("${app.jwt.issuer}") String issuer) throws Exception {
        RSAPublicKey key;
        try(var in=resource.getInputStream()){key=RsaKeyConverters.x509().convert(in);}
        var decoder=NimbusJwtDecoder.withPublicKey(key).build();
        OAuth2TokenValidator<Jwt> audience=new JwtClaimValidator<List<String>>("aud",a->a!=null&&a.contains("rental-api"));
        OAuth2TokenValidator<Jwt> required=jwt->jwt.getExpiresAt()!=null&&jwt.getSubject()!=null&&!jwt.getSubject().isBlank()
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Thieu exp/sub",null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),audience,required));
        return decoder;
    }
    @Bean JwtAuthenticationConverter jwtAuthenticationConverter() {
        var roles=new JwtGrantedAuthoritiesConverter();roles.setAuthoritiesClaimName("groups");roles.setAuthorityPrefix("ROLE_");
        var permissions=new JwtGrantedAuthoritiesConverter();permissions.setAuthoritiesClaimName("permissions");permissions.setAuthorityPrefix("");
        var converter=new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new DelegatingJwtGrantedAuthoritiesConverter(roles,permissions));return converter;
    }
    @Bean SecurityFilterChain apiFilter(HttpSecurity http,JwtAuthenticationConverter converter) throws Exception {
        return http.csrf(c->c.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rentals/quote/**").hasAuthority("quote:read")
                 .requestMatchers(HttpMethod.GET,"/api/availability").permitAll()
                .requestMatchers("/api/bookings","/api/bookings/**","/api/availability","/api/quotes","/api/owner/payment-settings").authenticated()
                .anyRequest().denyAll())
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req,res,ex)->unauthorized(res)))
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->unauthorized(res))
                .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Khong co quyen truy cap\"}");}))
            .build();
    }
    @Bean org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        var c=new org.springframework.web.cors.CorsConfiguration();
        c.setAllowedOrigins(List.of("http://localhost:8080","http://127.0.0.1:8080","http://localhost:8090","http://127.0.0.1:8090"));
        c.setAllowedMethods(List.of("GET","POST","PUT","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type","Idempotency-Key"));
        c.setExposedHeaders(List.of("X-CarManage-Instance"));
        var source=new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**",c);return source;
    }
    private static void unauthorized(jakarta.servlet.http.HttpServletResponse res) throws java.io.IOException {
        res.setStatus(401);res.setHeader("WWW-Authenticate","Bearer");res.setContentType("application/json");
        res.getWriter().write("{\"message\":\"Can dang nhap hoac JWT khong hop le\"}");
    }
}
