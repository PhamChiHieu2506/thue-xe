package com.example.carmanage_service.catalog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Offline snapshot: opening the application does not contact manufacturer websites. */
@Component
public class CarCatalog {
    private final List<CatalogEntry> entries;
    public CarCatalog() throws IOException {
        var provinces = new com.example.carmanage_service.location.ProvinceDirectory();
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("car-catalog.tsv").getInputStream(), StandardCharsets.UTF_8))) {
            entries = reader.lines().filter(s -> !s.isBlank() && !s.startsWith("#")).map(line -> {
                String[] f = line.split("\t", -1);
                if (f.length != 9) throw new IllegalStateException("Invalid catalog row");
                return new CatalogEntry(f[0],f[1],f[2],Integer.parseInt(f[3]),
                        f[4],f[5],f[6],new BigDecimal(f[7]),provinces.canonical(f[8]));
            }).toList();
        }
        if (entries.stream().map(CatalogEntry::id).distinct().count() != entries.size())
            throw new IllegalStateException("Duplicate model key");
    }
    public List<CatalogEntry> all() { return entries; }
    public boolean matchesBrand(String brand, com.example.carmanage_service.model.Car car) {
        if (car.getBrand().equalsIgnoreCase(brand)) return true;
        String modelId = CarImages.keyForCar(null, car.getName(), car.getBrand());
        return entries.stream().anyMatch(e -> e.id().equals(modelId) && e.brand().equalsIgnoreCase(brand));
    }
    public boolean matches(String modelId, com.example.carmanage_service.model.Car car) {
        CatalogEntry entry = require(modelId);
        return entry.id().equals(CarImages.keyForCar(null, car.getName(), car.getBrand()));
    }
    public CatalogEntry require(String id) {
        return entries.stream().filter(e -> e.id().equals(id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay mau xe: " + id));
    }
}
