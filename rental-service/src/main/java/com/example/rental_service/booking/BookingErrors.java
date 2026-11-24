package com.example.rental_service.booking;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestControllerAdvice
public class BookingErrors {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(BookingErrors.class);
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Không thực hiện được yêu cầu":e.getReason()));}
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) public ResponseEntity<?> duplicate(){return ResponseEntity.status(409).body(Map.of("message","Yêu cầu bị trùng. Hãy tải lại dữ liệu"));}
    @ExceptionHandler(feign.FeignException.class) public ResponseEntity<?> upstream(feign.FeignException e){int code=e.status()==404?404:e.status()==401||e.status()==403?e.status():e.status()<0||e.status()==503?503:502;for(Throwable c=e;c!=null;c=c.getCause())if(c instanceof java.net.SocketTimeoutException)code=504;log.warn("Car provider request failed with mapped HTTP status {}",code);return ResponseEntity.status(code).body(Map.of("message",code==404?"Không tìm thấy xe được đăng":code==503?"Dịch vụ xe chưa sẵn sàng, vui lòng thử lại":"Không lấy được dữ liệu xe (HTTP "+code+")"));}
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingRequestHeaderException.class,org.springframework.web.bind.MissingServletRequestParameterException.class}) public ResponseEntity<?> malformed(){return ResponseEntity.badRequest().body(Map.of("message","Thiếu thông tin hoặc dữ liệu không đúng định dạng"));}
}
