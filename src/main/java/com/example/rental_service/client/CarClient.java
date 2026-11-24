package com.example.rental_service.client;

import com.example.rental_service.dto.CarResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "carmanage-service")
public interface CarClient {
    @GetMapping("/api/cars/{id}")
    ResponseEntity<CarResponse> getCarById(
            @PathVariable("id") long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
