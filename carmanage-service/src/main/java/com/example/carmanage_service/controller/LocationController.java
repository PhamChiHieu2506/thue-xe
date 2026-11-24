package com.example.carmanage_service.controller;

import com.example.carmanage_service.location.ProvinceDirectory;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final ProvinceDirectory directory;
    public LocationController(ProvinceDirectory directory) { this.directory = directory; }
    @GetMapping("/provinces")
    public List<ProvinceDirectory.Province> provinces() { return directory.all(); }
}
