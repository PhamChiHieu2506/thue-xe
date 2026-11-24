package com.example.carmanage_service;
import com.example.testsupport.TestKeys;
import com.example.carmanage_service.repository.CarRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("test")
class CarAuthorizationTest {
    @Value("${local.server.port}") int port;
    @Autowired CarRepository repository;
    @DynamicPropertySource static void keys(DynamicPropertyRegistry props) {props.add("app.jwt.public-key-location",()->TestKeys.PUBLIC.toUri().toString());}
    final HttpClient http=HttpClient.newHttpClient();
    @Test void missingTamperedExpiredAndWrongIssuerAudienceTokensAre401() throws Exception {
        assertEquals(200,send("GET","/api/cars",null,null).statusCode());
        String valid=TestKeys.token("1","OWNER");int i=valid.lastIndexOf('.')+1;
        String tampered=valid.substring(0,i)+(valid.charAt(i)=='A'?'B':'A')+valid.substring(i+1);
        for(String token:new String[]{tampered,TestKeys.token("1","OWNER","https://ptpmhdv.local/auth","carmanage-api",-600),
                TestKeys.token("1","OWNER","https://wrong.example","carmanage-api",900),
                TestKeys.token("1","OWNER","https://ptpmhdv.local/auth","wrong-audience",900)}) {
            assertEquals(401,send("GET","/api/cars",token,null).statusCode());
        }
    }
    @Test void customerCannotCreateAndOwnerCannotSpoofOwnership() throws Exception {
        assertEquals(403,send("POST","/api/cars",TestKeys.token("customer","CUSTOMER"),body("30A-SEC01")).statusCode());
        var response=send("POST","/api/cars",TestKeys.token("owner-a","OWNER"),body("30A-SEC02"));
        assertEquals(201,response.statusCode());
        var stored=repository.findAll().stream().filter(c->"30A-SEC02".equals(c.getLicensePlate())).findFirst().orElseThrow();
        assertEquals("owner-a",stored.getOwnerId());
    }
    @Test void ownerCanUpdateOwnCarButOtherOwnerCannotUpdateOrDeleteAndAdminCan() throws Exception {
        var created=send("POST","/api/cars",TestKeys.token("owner-a","OWNER"),body("30A-SEC03"));
        String path=created.headers().firstValue("Location").orElseThrow();
        assertEquals(403,send("PUT",path,TestKeys.token("owner-b","OWNER"),body("30A-SEC03")).statusCode());
        assertEquals(403,send("DELETE",path,TestKeys.token("owner-b","OWNER"),null).statusCode());
        assertEquals(200,send("PUT",path,TestKeys.token("owner-a","OWNER"),body("30A-SEC03")).statusCode());
        long id=Long.parseLong(path.substring(path.lastIndexOf('/')+1));
        assertEquals("owner-a",repository.findById(id).orElseThrow().getOwnerId());
        assertEquals(200,send("DELETE",path,TestKeys.token("admin","ADMIN"),null).statusCode());
        assertEquals("ARCHIVED",repository.findById(id).orElseThrow().getListingStatus());
    }
    @Test void mineQueryIsBoundToCurrentOwnerAndCustomerCannotUseIt() throws Exception {
        send("POST","/api/cars",TestKeys.token("mine-a","OWNER"),body("30A-MINE01"));
        send("POST","/api/cars",TestKeys.token("mine-b","OWNER"),body("30A-MINE02"));
        var result=send("GET","/api/cars?mine=true",TestKeys.token("mine-a","OWNER"),null);
        assertEquals(200,result.statusCode());assertTrue(result.body().contains("30A-MINE01"));assertFalse(result.body().contains("30A-MINE02"));
        assertEquals(403,send("GET","/api/cars?mine=true",TestKeys.token("mine-customer","CUSTOMER"),null).statusCode());
        assertEquals(200,send("GET","/api/cars",TestKeys.token("mine-customer","CUSTOMER"),null).statusCode());
    }
    private String body(String plate) {return """
        {"licensePlate":"%s","name":"Toyota Vios","brand":"Toyota","seatCount":5,
         "pricePerDay":800000,"status":"AVAILABLE","ownerId":"spoofed-owner",
         "modelYear":2023,"city":"Hà Nội","district":"Khu vực kiểm thử","pickupAddress":"Địa chỉ riêng","description":"Thông tin xe do chủ xe nhập"}
        """.formatted(plate);}
    private HttpResponse<String> send(String method,String path,String token,String body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if(token!=null)builder.header("Authorization","Bearer "+token);
        if(body!=null)builder.header("Content-Type","application/json");
        return http.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
}
