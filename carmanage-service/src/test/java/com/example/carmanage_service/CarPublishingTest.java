package com.example.carmanage_service;

import com.example.testsupport.TestKeys;
import com.example.carmanage_service.model.Car;
import com.example.carmanage_service.repository.CarRepository;
import com.example.carmanage_service.service.DirectPublicationMigration;
import java.net.*;import java.net.http.*;import java.util.*;import java.io.*;
import java.awt.image.BufferedImage;import javax.imageio.ImageIO;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.beans.factory.annotation.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("test")
class CarPublishingTest {
 @Value("${local.server.port}") int port;
 @Autowired CarRepository repository;
 @Autowired DirectPublicationMigration migration;
 final HttpClient http=HttpClient.newHttpClient();final JsonMapper json=new JsonMapper();
 @DynamicPropertySource static void keys(DynamicPropertyRegistry p){p.add("app.jwt.public-key-location",()->TestKeys.PUBLIC.toUri().toString());}
 @Test void creatingAndUpdatingPublishesImmediatelyWithoutAdministratorApproval()throws Exception{
  var input=body("30A-DIRECT01",List.of());input.put("ownerId","spoofed");input.put("listingStatus","PENDING_REVIEW");
  var created=send("POST","/api/cars","direct-owner",input);assertEquals(201,created.statusCode(),created.body());var c=json.readTree(created.body());long id=c.get("id").asLong();
  assertEquals("PUBLISHED",c.get("listingStatus").asString());assertTrue(c.get("published").asBoolean());assertEquals("direct-owner",c.get("ownerId").asString());
  assertEquals(0,c.get("photoIds").size());assertEquals("/images/cars/toyota-vios.png",c.get("imageUrl").asString());
  var publicCar=send("GET","/api/cars/"+id,null,null);assertEquals(200,publicCar.statusCode());assertFalse(publicCar.body().contains("Private exact address"));
  assertTrue(send("GET","/api/cars?city=Ha%20Noi&model=toyota-vios",null,null).body().contains("Toyota Vios"));
  input.put("pricePerDay",900000);assertEquals(200,send("PUT","/api/cars/"+id,"direct-owner",input).statusCode());
  var updated=json.readTree(send("GET","/api/cars/"+id,null,null).body());assertTrue(updated.get("published").asBoolean());assertEquals(900000,updated.get("pricePerDay").intValue());
  assertEquals("direct-owner",repository.findById(id).orElseThrow().getOwnerId());
  assertEquals(410,send("POST","/api/cars/"+id+"/review","admin",Map.of("approve",true)).statusCode());
 }
 @Test void incompleteListingsAreRejectedAndUnknownModelsNeedAnUploadedImage()throws Exception{
  for(String field:List.of("city","district","pickupAddress","description","modelYear")){
   var input=body("30A-INCOMPLETE",List.of());input.remove(field);assertEquals(400,send("POST","/api/cars","direct-errors",input).statusCode(),field);
  }
  var input=body("30A-UNKNOWN",List.of());input.put("name","A different vehicle");input.put("brand","Custom brand");
  assertEquals(400,send("POST","/api/cars","image-owner",input).statusCode());
  String photo=upload("image-owner");input.put("photoIds",List.of(photo));assertEquals(201,send("POST","/api/cars","image-owner",input).statusCode());
  input.put("licensePlate","30A-STOLEN");assertEquals(403,send("POST","/api/cars","another-owner",input).statusCode());
 }
 @Test void existingPrivateDraftCanBePublishedByItsOwnerWithoutAReview()throws Exception{
  var c=fixture("30A-OLD-DRAFT","DRAFT",true);repository.saveAndFlush(c);String path="/api/cars/"+c.getId();
  assertEquals(404,send("GET",path,null,null).statusCode());assertEquals(403,send("POST",path+"/publish","other-owner",null).statusCode());
  var published=send("POST",path+"/publish","legacy-owner",null);assertEquals(200,published.statusCode(),published.body());assertEquals(200,send("GET",path,null,null).statusCode());
  assertEquals(200,send("POST",path+"/submit","legacy-owner",null).statusCode());assertEquals("PUBLISHED",repository.findById(c.getId()).orElseThrow().getListingStatus());
 }
 @Test void legacyPendingMigrationIsIdempotentAndPreservesOwnershipPriceAndIncompleteDrafts()throws Exception{
  var ready=fixture("30A-OLD-READY","PENDING_REVIEW",true);repository.saveAndFlush(ready);
  var incomplete=fixture("30A-OLD-MISSING","PENDING_REVIEW",false);repository.saveAndFlush(incomplete);
  var draft=fixture("30A-KEEP-DRAFT","DRAFT",true);repository.saveAndFlush(draft);
  var archived=fixture("30A-ARCHIVED","ARCHIVED",true);repository.saveAndFlush(archived);
  var sample=fixture("OLD-SAMPLE","PENDING_REVIEW",true);sample.setLegacySample(true);repository.saveAndFlush(sample);
  long count=repository.count();migration.run();migration.run();var actual=repository.findById(ready.getId()).orElseThrow();
  assertEquals("PUBLISHED",actual.getListingStatus());assertEquals("legacy-owner",actual.getOwnerId());assertEquals(0,new java.math.BigDecimal("800000").compareTo(actual.getPricePerDay()));assertEquals("Private exact address",actual.getPickupAddress());
  assertEquals("PENDING_REVIEW",repository.findById(incomplete.getId()).orElseThrow().getListingStatus());assertEquals("DRAFT",repository.findById(draft.getId()).orElseThrow().getListingStatus());assertEquals("ARCHIVED",repository.findById(archived.getId()).orElseThrow().getListingStatus());assertTrue(repository.findById(sample.getId()).orElseThrow().isLegacySample());assertEquals(count,repository.count());
  assertEquals(400,send("POST","/api/cars/"+incomplete.getId()+"/publish","legacy-owner",null).statusCode());
 }
 @Test void publicMetadataDoesNotCreateVehiclesAndRetainsPricesLocationsAndImages()throws Exception{
  long before=repository.count();var locations=send("GET","/api/locations/provinces",null,null);assertEquals(200,locations.statusCode());assertEquals(34,json.readTree(locations.body()).size());
  var result=send("GET","/api/car-catalog",null,null);assertEquals(200,result.statusCode());var models=json.readTree(result.body());assertEquals(21,models.size());
  for(var m:models){assertTrue(m.get("displayPricePerDay").decimalValue().signum()>0);assertFalse(m.get("displayCity").asString().isBlank());assertEquals(200,send("GET",m.get("imageUrl").asString(),null,null).statusCode());}
  assertEquals(before,repository.count());
 }
 @Test void legacySamplesRemainHiddenAndTheirMarkerCannotBeForgedByAClient()throws Exception{
  var sample=fixture("OLD-SAMPLE-HIDE","PUBLISHED",true);sample.setLegacySample(true);repository.saveAndFlush(sample);
  assertFalse(send("GET","/api/cars",null,null).body().contains("OLD-SAMPLE-HIDE"));assertEquals(404,send("GET","/api/cars/"+sample.getId(),null,null).statusCode());
  var input=body("30A-FORGERY",List.of());input.put("legacySample",true);var created=send("POST","/api/cars","direct-forge",input);assertEquals(201,created.statusCode());long id=json.readTree(created.body()).get("id").asLong();assertFalse(repository.findById(id).orElseThrow().isLegacySample());
 }
 Car fixture(String plate,String status,boolean complete){Car c=new Car(null,plate,"Toyota Vios","Toyota",5,new java.math.BigDecimal("800000"),"AVAILABLE");c.setOwnerId("legacy-owner");c.setListingStatus(status);if(complete){c.setCity("Hà Nội");c.setDistrict("Khu vực kiểm thử");c.setPickupAddress("Private exact address");c.setDescription("Thông tin xe của chủ xe");c.setModelYear(2023);}return c;}
 Map<String,Object> body(String plate,List<String> photos){var m=new LinkedHashMap<String,Object>();m.put("licensePlate",plate);m.put("name","Toyota Vios");m.put("brand","Toyota");m.put("seatCount",5);m.put("pricePerDay",800000);m.put("status","AVAILABLE");m.put("modelYear",2023);m.put("city","Hà Nội");m.put("district","Khu vực kiểm thử");m.put("pickupAddress","Private exact address");m.put("description","Xe của chủ xe");m.put("photoIds",photos);return m;}
 String upload(String owner)throws Exception{var png=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(300,200,BufferedImage.TYPE_INT_RGB),"png",png);String boundary="direct-publication-boundary";var data=new ByteArrayOutputStream();data.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"car.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes());data.write(png.toByteArray());data.write(("\r\n--"+boundary+"--\r\n").getBytes());var q=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/car-media")).header("Authorization","Bearer "+TestKeys.token(owner,"OWNER")).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(data.toByteArray())).build();var response=http.send(q,HttpResponse.BodyHandlers.ofString());assertEquals(201,response.statusCode(),response.body());return json.readTree(response.body()).get("id").asString();}
 HttpResponse<String> send(String method,String path,String actor,Object body)throws Exception{var q=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));if(actor!=null)q.header("Authorization","Bearer "+TestKeys.token(actor,actor.equals("admin")?"ADMIN":"OWNER"));if(body!=null)q.header("Content-Type","application/json");return http.send(q.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
}
