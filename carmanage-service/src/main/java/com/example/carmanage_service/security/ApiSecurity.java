package com.example.carmanage_service.security;
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
        OAuth2TokenValidator<Jwt> audience=new JwtClaimValidator<List<String>>("aud",a->a!=null&&a.contains("carmanage-api"));
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
        return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers("/actuator/health","/actuator/health/**","/dashboard.html","/","/index.html","/media/**","/css/**","/js/**","/images/**","/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/car-catalog","/api/locations/provinces").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/car-media","/api/cars/*/publish","/api/cars/*/submit","/api/cars/*/review").hasAnyAuthority("car:write:own","car:write:any")
                .requestMatchers(HttpMethod.GET,"/api/cars","/api/cars/**").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/cars").hasAnyAuthority("car:write:own","car:write:any")
                .requestMatchers(HttpMethod.PUT,"/api/cars/**").hasAnyAuthority("car:write:own","car:write:any")
                .requestMatchers(HttpMethod.DELETE,"/api/cars/**").hasAnyAuthority("car:write:own","car:write:any")
                .anyRequest().denyAll())
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req,res,ex)->unauthorized(res)))
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->unauthorized(res))
                .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Khong co quyen truy cap\"}");}))
            .build();
    }
    private static void unauthorized(jakarta.servlet.http.HttpServletResponse res) throws java.io.IOException {
        res.setStatus(401);res.setHeader("WWW-Authenticate","Bearer");res.setContentType("application/json");
        res.getWriter().write("{\"message\":\"Can dang nhap hoac JWT khong hop le\"}");
    }
}
