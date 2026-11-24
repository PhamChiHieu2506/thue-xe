package com.example.carmanage_service.controller;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Không thực hiện được yêu cầu":e.getReason()));}
    @ExceptionHandler(NoSuchElementException.class) public ResponseEntity<?> missing(NoSuchElementException e){return ResponseEntity.status(404).body(Map.of("message",e.getMessage()));}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
    @ExceptionHandler(IllegalStateException.class) public ResponseEntity<?> conflict(IllegalStateException e){return ResponseEntity.status(409).body(Map.of("message",e.getMessage()));}
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class}) public ResponseEntity<?> json(){return ResponseEntity.badRequest().body(Map.of("message","Dữ liệu gửi lên không hợp lệ"));}
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) public ResponseEntity<?> size(){return ResponseEntity.status(413).body(Map.of("message","Ảnh vượt quá dung lượng cho phép"));}
}
