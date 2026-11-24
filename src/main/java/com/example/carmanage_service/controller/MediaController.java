package com.example.carmanage_service.controller;
import com.example.carmanage_service.service.PhotoService;
import java.util.*;
import java.io.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
@RestController
public class MediaController {
    private final PhotoService photos;
    public MediaController(PhotoService photos){this.photos=photos;}
    @PostMapping("/api/car-media") @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<?> upload(@RequestParam MultipartFile file,@AuthenticationPrincipal Jwt jwt)throws IOException{var p=photos.save(file,jwt.getSubject());return ResponseEntity.status(201).body(Map.of("id",p.id,"url","/media/"+p.id));}
    @GetMapping("/media/{id}") public ResponseEntity<?> image(@PathVariable String id){var p=photos.find(id);var file=new FileSystemResource(photos.path(p));if(!file.exists())return ResponseEntity.notFound().build();return ResponseEntity.ok().contentType(MediaType.parseMediaType(p.contentType)).header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)).cachePublic()).body(file);}
}
