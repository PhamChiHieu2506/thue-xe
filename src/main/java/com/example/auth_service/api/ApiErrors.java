package com.example.auth_service.api;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
 public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason()==null?"Không thể thực hiện yêu cầu":ex.getReason()));}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
 public ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Map.of("message","Thông tin bị trùng hoặc không hợp lệ"));}
}
