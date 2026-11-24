package com.example.rental_service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.testsupport.TestKeys;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:rental_http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa",
        "spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.openfeign.client.config.carmanage-service.readTimeout=1000"
})
class RentalIntegrationTest {
    private static final AtomicReference<String> FORWARDED = new AtomicReference<>();
    private static final HttpServer PROVIDER = startProvider();
    private static final String TOKEN = TestKeys.token("42", "CUSTOMER");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final JsonMapper JSON = new JsonMapper();

    @LocalServerPort int port;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry props) {
        props.add("app.jwt.public-key-location", () -> TestKeys.PUBLIC.toUri().toString());
        props.add("spring.cloud.discovery.client.simple.instances.carmanage-service[0].uri",
                () -> "http://127.0.0.1:" + PROVIDER.getAddress().getPort());
    }

    @AfterAll
    static void stopProvider() {
        PROVIDER.stop(0);
    }

    @Test
    void quotesThroughNamedProviderAndForwardsJwt() throws Exception {
        var r = get("/api/rentals/quote/1?days=3", TOKEN);
        assertEquals(200, r.statusCode(), r.body());
        assertEquals(2400000, JSON.readTree(r.body()).get("totalPrice").intValue());
        assertEquals("mock-car-1", r.headers().firstValue("X-CarManage-Instance").orElseThrow());
        assertEquals("Bearer " + TOKEN, FORWARDED.get());
    }

    @Test
    void quotesCarAndIgnoresAdditionalCatalogMetadata() throws Exception {
        var r=get("/api/rentals/quote/7?days=3",TOKEN);
        assertEquals(200,r.statusCode(),r.body());
        assertEquals(1350000,JSON.readTree(r.body()).get("totalPrice").intValue());
        assertTrue(r.body().contains("VinFast VF 3"));
    }

    @Test
    void validatesInputsAndVehicleStatus() throws Exception {
        assertEquals(400, get("/api/rentals/quote/0?days=3", TOKEN).statusCode());
        assertEquals(400, get("/api/rentals/quote/1?days=0", TOKEN).statusCode());
        assertEquals(400, get("/api/rentals/quote/1", TOKEN).statusCode());
        assertEquals(409, get("/api/rentals/quote/2?days=3", TOKEN).statusCode());
    }

    @Test
    void mapsUpstreamHttpErrorsAndMalformedJson() throws Exception {
        assertEquals(404, get("/api/rentals/quote/999?days=3", TOKEN).statusCode());
        assertEquals(502, get("/api/rentals/quote/500?days=3", TOKEN).statusCode());
        assertEquals(502, get("/api/rentals/quote/5?days=3", TOKEN).statusCode());
        assertEquals(503, get("/api/rentals/quote/503?days=3", TOKEN).statusCode());
    }

    @Test
    void reportsTimeout() throws Exception {
        assertEquals(504, get("/api/rentals/quote/6?days=3", TOKEN).statusCode());
    }

    @Test
    void protectsBusinessButExposesHealth() throws Exception {
        assertEquals(401, get("/api/rentals/quote/1?days=3", null).statusCode());
        assertEquals(401, get("/api/rentals/quote/1?days=3", TOKEN + "bad").statusCode());
        assertEquals(403, get("/api/rentals/quote/1?days=3", TestKeys.token("42", "GUEST")).statusCode());
        assertEquals(200, get("/actuator/health", null).statusCode());
    }

    @Test void browserQuotePreflightAndResponseAllowDashboardAndExposeInstance() throws Exception {
        var preflight=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/rentals/quote/1?days=3"))
                .header("Origin","http://localhost:8080").header("Access-Control-Request-Method","GET")
                .header("Access-Control-Request-Headers","authorization").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(200,HTTP.send(preflight,HttpResponse.BodyHandlers.ofString()).statusCode());
        var response=HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/rentals/quote/1?days=3"))
                .header("Origin","http://localhost:8080").header("Authorization","Bearer "+TOKEN).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());assertEquals("http://localhost:8080",response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertTrue(response.headers().firstValue("Access-Control-Expose-Headers").orElseThrow().contains("X-CarManage-Instance"));
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10));
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(req.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer startProvider() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/cars/", RentalIntegrationTest::respond);
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void respond(HttpExchange ex) throws IOException {
        try {
            String id = ex.getRequestURI().getPath().substring("/api/cars/".length());
            FORWARDED.set(ex.getRequestHeaders().getFirst("Authorization"));
            if (id.equals("6")) {
                try {
                    Thread.sleep(1600);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            int status = switch (id) {
                case "999" -> 404;
                case "500" -> 500;
                case "503" -> 503;
                default -> 200;
            };
            String vehicleStatus = id.equals("2") ? "RENTED" : "AVAILABLE";
            String result = status == 200 ? """
                    {"id":%s,"name":"Toyota Vios","status":"%s","pricePerDay":800000}
                    """.formatted(id, vehicleStatus) : "{}";
            if (id.equals("7")) {
                result="""
                    {"id":7,"name":"VinFast VF 3","brand":"VinFast","status":"AVAILABLE",
                     "pricePerDay":450000,"seatCount":4,"ownerId":"42","licensePlate":"TEST-7",
                     "published":true,"catalogKey":"vinfast-vf3",
                     "sourceUrl":"https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf3","sourceVerifiedAt":"2026-11-20"}
                    """;
            }
            if (id.equals("5")) {
                result = "{";
            }
            byte[] body = result.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("X-Service-Instance", "mock-car-1");
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, body.length);
            ex.getResponseBody().write(body);
        } finally {
            ex.close();
        }
    }
}
