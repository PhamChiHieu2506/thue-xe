package com.example.carmanage_service;

import com.example.carmanage_service.location.ProvinceDirectory;
import com.example.carmanage_service.catalog.CarCatalog;
import com.example.carmanage_service.catalog.CarImages;
import com.example.carmanage_service.model.Car;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProvinceDirectoryTest {
    @Test void containsAll34UniqueProvincesAndSixCities() throws Exception {
        var directory = new ProvinceDirectory();
        assertEquals(34, directory.all().size());
        assertEquals(34, directory.all().stream().map(ProvinceDirectory.Province::code).distinct().count());
        assertEquals(6, directory.all().stream().filter(p -> p.type().equals("CITY")).count());
        assertEquals("Thành phố Hà Nội", directory.resolve("01").orElseThrow().getLabel());
        assertEquals("Tỉnh Cà Mau", directory.resolve("96").orElseThrow().getLabel());
    }
    @Test void legacyNamesAndAccentlessSearchResolveWithoutChangingStoredRecords() throws Exception {
        var directory = new ProvinceDirectory();
        assertTrue(directory.matches("Bình Dương", "Hồ Chí Minh"));
        assertTrue(directory.matches("Tỉnh Yên Bái", "Lào Cai"));
        assertTrue(directory.matches("Ha Noi", "Thành phố Hà Nội"));
        assertTrue(directory.matches("TP.HCM", "79"));
        assertEquals("Lâm Đồng", directory.canonical("Đắk Nông"));
        assertEquals("Phú Thọ", directory.canonical("Hòa Bình"));
        assertFalse(directory.matches("Hà Nội", "Hải Phòng"));
    }
    @Test void unknownNewProvinceIsRejectedButEmptyDraftLocationIsPreserved() throws Exception {
        var directory = new ProvinceDirectory();
        assertThrows(IllegalArgumentException.class, () -> directory.canonical("Nonexistent province"));
        assertNull(directory.canonical(null));assertEquals("", directory.canonical(""));
        assertTrue(directory.matches(null, ""));assertFalse(directory.matches(null, "Hà Nội"));
    }
    @Test void catalogModelsHaveDeterministicPhotosAndConfiguredDisplaySettings() throws Exception {
        var catalog = new CarCatalog();
        assertEquals(21, catalog.all().size());
        assertEquals(6, catalog.all().stream().map(e -> e.brand()).distinct().count());
        for (var entry : catalog.all()) {
            assertTrue(entry.getImageUrl().startsWith("/images/cars/"));
            assertNotNull(getClass().getResource("/static" + entry.getImageUrl()), entry.id());
            assertTrue(entry.displayPricePerDay().signum() > 0);
            assertTrue(new ProvinceDirectory().resolve(entry.displayCity()).isPresent());
        }
        assertEquals("/images/cars/toyota-vios.png", CarImages.forCar(null, "Toyota Vios", "Toyota"));
        assertEquals("/images/cars/mercedes-g63.jpg", CarImages.forCar(null, "G63", "mecedes"));
        assertEquals(CarImages.FALLBACK, CarImages.forCar(null, "Unknown", "Unknown"));
    }
    @Test void modelMatchingUsesTheVehicleNotThePictureUploadedByAnOwner() throws Exception {
        var catalog = new CarCatalog();
        var car = new Car(1L, "TEST-PLATE", "Toyota Vios 2023", "Toyota", 5, new BigDecimal("800000"), "AVAILABLE");
        car.setPhotoIds(List.of("0123456789abcdef0123456789abcdef"));
        assertTrue(catalog.matches("toyota-vios", car));
        assertFalse(catalog.matches("vinfast-vf3", car));
        assertEquals("toyota-vios", CarImages.keyForCar(null, car.getName(), car.getBrand()));
        var legacy = new Car(2L, "TEST-G63", "G63", "mecedes", 5, new BigDecimal("1"), "AVAILABLE");
        assertTrue(catalog.matchesBrand("Mercedes-Benz", legacy));
        assertFalse(catalog.matchesBrand("Toyota", legacy));
    }
}
