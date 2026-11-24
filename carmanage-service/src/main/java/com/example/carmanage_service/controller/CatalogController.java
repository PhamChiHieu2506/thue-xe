package com.example.carmanage_service.controller;
import com.example.carmanage_service.catalog.*;
import org.springframework.web.bind.annotation.*;
@RestController public class CatalogController {
    private final CarCatalog catalog;public CatalogController(CarCatalog catalog){this.catalog=catalog;}
    @GetMapping("/api/car-catalog") public java.util.List<CatalogEntry> list(){return catalog.all();}
}
