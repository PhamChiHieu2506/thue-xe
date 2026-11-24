package com.example.rental_service.resource;

import com.example.rental_service.client.CarClient;
import com.example.rental_service.dto.CarResponse;
import feign.FeignException;
import feign.codec.DecodeException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rentals")
public class RentalQuoteResource {
    private static final Logger log = LoggerFactory.getLogger(RentalQuoteResource.class);
    private final CarClient carClient;

    public RentalQuoteResource(CarClient carClient) {
        this.carClient = carClient;
    }

    @GetMapping("/quote/{carId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'CUSTOMER')")
    public ResponseEntity<Map<String, Object>> calculateQuote(
            @PathVariable long carId,
            @RequestParam(required = false) Integer days,
            @AuthenticationPrincipal Jwt jwt) {
        if (carId <= 0) {
            return error(400, "Ma xe phai lon hon 0");
        }
        if (days == null || days <= 0) {
            return error(400, "So ngay thue phai lon hon 0");
        }

        try {
            ResponseEntity<CarResponse> upstream = carClient.getCarById(carId, "Bearer " + jwt.getTokenValue());
            CarResponse car = upstream.getBody();
            if (upstream.getStatusCode().value() != 200
                    || car == null || car.getId() == null || car.getId() != carId
                    || car.getName() == null || car.getName().isBlank()
                    || car.getPricePerDay() == null || car.getPricePerDay().signum() <= 0
                    || car.getStatus() == null) {
                log.warn("CarManage returned invalid data for carId={}", carId);
                return error(502, "Service quan ly xe tra ve du lieu khong hop le");
            }
            if (!"AVAILABLE".equalsIgnoreCase(car.getStatus())) {
                return error(409, "Xe hien khong san sang cho thue");
            }

            BigDecimal totalPrice = car.getPricePerDay().multiply(BigDecimal.valueOf(days));
            String instance = upstream.getHeaders().getFirst("X-Service-Instance");
            ResponseEntity.BodyBuilder response = ResponseEntity.ok();
            if (instance != null) {
                response.header("X-CarManage-Instance", instance);
            }
            log.info("Quote calculated carId={} days={} carManageInstance={}", carId, days, instance);
            return response.body(Map.of(
                    "carId", car.getId(), "carName", car.getName(), "days", days,
                    "pricePerDay", car.getPricePerDay(), "totalPrice", totalPrice, "currency", "VND"));
        } catch (DecodeException ex) {
            log.warn("CarManage returned unreadable data for carId={}", carId);
            return error(502, "Service quan ly xe tra ve du lieu khong hop le");
        } catch (FeignException ex) {
            int status = ex.status();
            if (status == 404) {
                return error(404, "Khong tim thay xe co id = " + carId);
            }
            if (status == 401 || status == 403) {
                return error(status, "CarManage tu choi quyen truy cap");
            }
            if (status == 504 || (status < 0 && isTimeout(ex))) {
                log.warn("CarManage timeout for carId={}", carId);
                return error(504, "Het thoi gian cho phan hoi tu CarManage");
            }
            if (status < 0 || status == 503) {
                log.warn("CarManage unavailable for carId={} upstreamStatus={}", carId, status);
                return error(503, "Khong tim duoc instance CarManage kha dung hoac khong the ket noi dich vu");
            }
            log.warn("CarManage request failed for carId={} upstreamStatus={}", carId, status);
            return error(502, "Service quan ly xe tra ve loi HTTP " + status);
        }
    }

    private static boolean isTimeout(Throwable ex) {
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException
                    || cause instanceof TimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static ResponseEntity<Map<String, Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
