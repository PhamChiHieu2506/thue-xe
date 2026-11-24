package com.example.testsupport;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.time.Instant;

/** Ephemeral test keys, never production credentials. */
public final class TestKeys {
    public static final KeyPair PAIR;
    public static final Path PUBLIC, PRIVATE;
    static {
        try {
            var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);PAIR=generator.generateKeyPair();
            var dir=Files.createTempDirectory("ptpmhdv-jwt-test-");
            PUBLIC=dir.resolve("public.pem");PRIVATE=dir.resolve("private.pem");
            Files.writeString(PUBLIC,pem("PUBLIC KEY",PAIR.getPublic().getEncoded()));
            Files.writeString(PRIVATE,pem("PRIVATE KEY",PAIR.getPrivate().getEncoded()));
        } catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    public static String pem(String type,byte[] bytes) {
        return "-----BEGIN "+type+"-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(bytes)+"\n-----END "+type+"-----\n";
    }
    public static String token(String subject,String role) {return token(subject,role,"https://ptpmhdv.local/auth","carmanage-api\",\"rental-api",900);}
    public static String token(String subject,String role,String issuer,String audience,long lifetime) {
        try {
            long now=Instant.now().getEpochSecond();
            String permissions=switch(role) {
                case "ADMIN" -> "\"car:read\",\"car:write:any\",\"quote:read\"";
                case "OWNER" -> "\"car:read\",\"car:write:own\",\"quote:read\"";
                case "CUSTOMER" -> "\"car:read\",\"quote:read\"";
                default -> "";
            };
            String payload="{\"iss\":\""+issuer+"\",\"sub\":\""+subject+"\",\"upn\":\"test-"+subject+"\",\"aud\":[\""+audience+"\"],\"iat\":"+(now-1200)+",\"exp\":"+(now+lifetime)+",\"groups\":[\""+role+"\"],\"permissions\":["+permissions+"]}";
            var b64=Base64.getUrlEncoder().withoutPadding();
            String unsigned=b64.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))+"."+b64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            var signer=Signature.getInstance("SHA256withRSA");signer.initSign(PAIR.getPrivate());signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
            return unsigned+"."+b64.encodeToString(signer.sign());
        }catch(Exception e){throw new IllegalStateException(e);}
    }
}
