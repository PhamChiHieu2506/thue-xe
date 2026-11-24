package com.example.auth_service.security;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
public class JwtKeys {
    @Bean RSAPublicKey publicKey(@Value("${app.jwt.public-key-location}") Resource resource) throws Exception {
        try(var in=resource.getInputStream()){return RsaKeyConverters.x509().convert(in);}
    }
    @Bean RSAPrivateKey privateKey(@Value("${app.jwt.private-key-location}") Resource resource) throws Exception {
        try(var in=resource.getInputStream()){return RsaKeyConverters.pkcs8().convert(in);}
    }
    @Bean JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        var key=new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("ptpmhdv-rs256").build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
}
