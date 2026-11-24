package com.example.carmanage_service;

import com.example.carmanage_service.model.Car;
import com.example.testsupport.TestKeys;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.example.carmanage_service.repository.CarRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CarmanageServiceApplicationTests {
    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry props) { props.add("app.jwt.public-key-location",()->TestKeys.PUBLIC.toUri().toString()); }
    @Value("${local.server.port}") int port;
    @Autowired CarRepository repository;
    final HttpClient http = HttpClient.newHttpClient();

    @Test
    void createCommitsToDatabaseAndCanBeReadThroughHttp() throws Exception {
        var created = post("30A-99001");
        assertEquals(201, created.statusCode());
        String location = created.headers().firstValue("Location").orElseThrow();
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        Car stored = repository.findById(id).orElseThrow();
        assertEquals("Toyota Vios", stored.getName());
        assertEquals(0, new BigDecimal("800000.00").compareTo(stored.getPricePerDay()));
        var read = http.send(HttpRequest.newBuilder(uri(location)).header("Authorization", "Bearer "+TestKeys.token("admin-test","ADMIN")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, read.statusCode());
        assertTrue(read.body().contains("30A-99001"));
        assertEquals("carmanage-test", read.headers().firstValue("X-Service-Instance").orElseThrow());
    }

    @Test
    void duplicatePlateReturnsConflict() throws Exception {
        assertEquals(201, post("30A-99002").statusCode());
        assertEquals(409, post("30A-99002").statusCode());
    }

    @Test
    void invalidCarReturnsBadRequest() throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/cars"))
                .header("Authorization", "Bearer "+TestKeys.token("admin-test","ADMIN")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")) .build();
        assertEquals(400, http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void missingCarReturnsNotFound() throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/cars/99999999")).header("Authorization", "Bearer "+TestKeys.token("admin-test","ADMIN")).GET().build();
        assertEquals(404, http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void actuatorReportsHealthyDatabase() throws Exception {
        var response = http.send(HttpRequest.newBuilder(uri("/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void existingDashboardIsPackagedAsStaticResource() throws Exception {
        var page = http.send(HttpRequest.newBuilder(uri("/dashboard.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("VeloDrive"));
        var image = http.send(HttpRequest.newBuilder(uri("/images/hero-drive.png")).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(200, image.statusCode());
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }

    private HttpResponse<String> post(String plate) throws Exception {
        String json = """
                {"licensePlate":"%s","name":"Toyota Vios","brand":"Toyota",
                 "seatCount":5,"pricePerDay":800000,"status":"AVAILABLE",
                 "modelYear":2023,"city":"Hà Nội","district":"Khu vực kiểm thử","pickupAddress":"Địa chỉ riêng","description":"Thông tin xe do chủ xe nhập"}
                """.formatted(plate);
        var request = HttpRequest.newBuilder(uri("/api/cars"))
                .header("Authorization", "Bearer "+TestKeys.token("admin-test","ADMIN")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
