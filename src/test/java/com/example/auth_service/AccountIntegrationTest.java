package com.example.auth_service;
import com.example.auth_service.account.*;
import com.example.testsupport.TestKeys;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import java.net.*;import java.net.http.*;import java.util.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("test")
class AccountIntegrationTest {
 @Value("${local.server.port}")int port;@Autowired AccountRepository accounts;@Autowired PasswordEncoder passwords;@Autowired BootstrapAdmin bootstrap;
 final HttpClient http=HttpClient.newHttpClient();final JsonMapper json=new JsonMapper();
 @DynamicPropertySource static void keys(DynamicPropertyRegistry p){p.add("app.jwt.public-key-location",()->TestKeys.PUBLIC.toUri().toString());p.add("app.jwt.private-key-location",()->TestKeys.PRIVATE.toUri().toString());}
 Map<String,Object> registration(String user,String role){return Map.of("username",user,"password","123456","role",role,"displayName","Nguyễn Văn An","phone","0901234567","email","nguyenvanan@example.com");}
 @Test void registrationLoginAndPrivacy()throws Exception{
  var created=send("POST","/api/auth/register",null,registration("real-owner","OWNER"));assertEquals(201,created.statusCode(),created.body());
  assertFalse(created.body().contains("0901234567"));assertTrue(created.body().contains("*******567"));
  var login=login("real-owner","123456");assertEquals(200,login.statusCode());String token=json.readTree(login.body()).get("accessToken").asString();
  var jwt=org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey)TestKeys.PAIR.getPublic()).build().decode(token);
  assertTrue(jwt.getClaimAsStringList("groups").contains("OWNER"));assertTrue(jwt.getClaimAsStringList("permissions").contains("car:write:own"));
  var account=accounts.findByUsername("real-owner").orElseThrow();assertTrue(passwords.matches("123456",account.getPasswordHash()));assertFalse(login.body().contains("password"));
  var profile=send("GET","/api/auth/me",token,null);assertEquals(200,profile.statusCode());assertEquals("no-store",profile.headers().firstValue("Cache-Control").orElseThrow());assertFalse(profile.body().contains("nguyenvanan@example.com"));
 }
 @Test void duplicateRoleEscalationAndInvalidInputsRejected()throws Exception{
  assertEquals(201,send("POST","/api/auth/register",null,registration("unique-person","CUSTOMER")).statusCode());
  assertEquals(409,send("POST","/api/auth/register",null,registration("UNIQUE-PERSON","CUSTOMER")).statusCode());
  assertEquals(400,send("POST","/api/auth/register",null,registration("forged-admin","ADMIN")).statusCode());
  var invalid=new HashMap<>(registration("bad-phone","CUSTOMER"));invalid.put("phone","***123");assertEquals(400,send("POST","/api/auth/register",null,invalid).statusCode());
  assertEquals(401,login("unknown-account","123456").statusCode());assertEquals(401,login("admin","wrong").statusCode());
 }
 @Test void passwordChangesRequireCurrentPassword()throws Exception{
  send("POST","/api/auth/register",null,registration("password-owner","OWNER"));String token=json.readTree(login("password-owner","123456").body()).get("accessToken").asString();
  assertEquals(400,send("PUT","/api/auth/password",token,Map.of("currentPassword","wrong","newPassword","654321")).statusCode());
  assertEquals(200,send("PUT","/api/auth/password",token,Map.of("currentPassword","123456","newPassword","654321")).statusCode());
  assertEquals(401,login("password-owner","123456").statusCode());assertEquals(200,login("password-owner","654321").statusCode());
 }
 @Test void profileIsBoundToSubjectAndMaskedFieldsAreNotAcceptedAsRawInput()throws Exception{
  send("POST","/api/auth/register",null,registration("profile-owner","OWNER"));String token=json.readTree(login("profile-owner","123456").body()).get("accessToken").asString();
  assertEquals(200,send("PUT","/api/auth/me",token,Map.of("role","ADMIN","id",1,"phone","0988888123")).statusCode());assertEquals("OWNER",accounts.findByUsername("profile-owner").orElseThrow().getRole());
  assertEquals(400,send("PUT","/api/auth/me",token,Map.of("phone","***123")).statusCode());assertEquals(401,send("GET","/api/auth/me",null,null).statusCode());
 }
 @Test void bootstrapDoesNotResetExistingAdminPasswordOrAddAccounts()throws Exception{
  var a=accounts.findByUsername("admin").orElseThrow();String hash=a.getPasswordHash();long count=accounts.count();bootstrap.run();assertEquals(hash,accounts.findById(a.getId()).orElseThrow().getPasswordHash());assertEquals(count,accounts.count());
 }
 @Test void onlyActiveAdminCanManageAccounts()throws Exception{
  send("POST","/api/auth/register",null,registration("locked-customer","CUSTOMER"));String customer=json.readTree(login("locked-customer","123456").body()).get("accessToken").asString();assertEquals(403,send("GET","/api/admin/accounts",customer,null).statusCode());
  String admin=json.readTree(login("admin","Test-admin-only-2026!").body()).get("accessToken").asString();
  var listed=send("GET","/api/admin/accounts",admin,null);assertTrue(listed.body().contains("phoneMasked"));assertFalse(listed.body().contains("0901234567"));long id=accounts.findByUsername("locked-customer").orElseThrow().getId();
  assertEquals(200,send("PUT","/api/admin/accounts/"+id+"/enabled",admin,Map.of("enabled",false)).statusCode());assertEquals(401,login("locked-customer","123456").statusCode());assertEquals(401,send("GET","/api/auth/me",customer,null).statusCode());
 }
 @Test void corsAcceptsOnlyConfiguredOrigins()throws Exception{for(String origin:List.of("http://localhost:8080","http://untrusted.example")){var q=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/auth/register")).header("Origin",origin).header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","content-type").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();assertEquals(origin.contains("localhost")?200:403,http.send(q,HttpResponse.BodyHandlers.ofString()).statusCode());}}
 HttpResponse<String> login(String u,String p)throws Exception{return send("POST","/api/auth/login",null,Map.of("username",u,"password",p));}
 HttpResponse<String> send(String method,String path,String token,Object body)throws Exception{var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));if(token!=null)b.header("Authorization","Bearer "+token);if(body!=null)b.header("Content-Type","application/json");return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
}
