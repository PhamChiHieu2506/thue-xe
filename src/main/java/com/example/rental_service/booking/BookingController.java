package com.example.rental_service.booking;
import java.util.*;
import java.time.Instant;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
@RestController
public class BookingController {
    private final BookingService service;
    public BookingController(BookingService service){this.service=service;}
    private Map<String,Object> view(Booking b){Map<String,Object> m=new LinkedHashMap<>();m.put("id",b.id);m.put("carId",b.carId);m.put("carName",b.carName);m.put("imageUrl",b.imageUrl);m.put("customerId",b.customerId);m.put("ownerId",b.ownerId);m.put("startsAt",b.startsAt);m.put("endsAt",b.endsAt);m.put("days",b.days);m.put("pricePerDay",b.pricePerDay);m.put("totalPrice",b.totalPrice);m.put("deposit",b.deposit);m.put("paid",b.paid);m.put("remaining",b.totalPrice.subtract(b.paid));m.put("status",service.visibleStatus(b));m.put("message",b.message);m.put("reason",b.reason);m.put("createdAt",b.createdAt);m.put("paymentDeadline",b.paymentDeadline);m.put("actualStart",b.actualStart);m.put("actualEnd",b.actualEnd);m.put("refundDue",b.refundDue);m.put("refundedAt",b.refundedAt);m.put("refundReference",b.refundReference);m.put("handoverAddress",Set.of("CONFIRMED","IN_PROGRESS","COMPLETED").contains(b.status)||b.ownerId.equals(service.currentSubject())?b.handoverAddress:null);return m;}
    @GetMapping("/api/availability") public Map<String,Object> availability(@RequestParam Instant startsAt,@RequestParam Instant endsAt){return Map.of("unavailableCarIds",service.unavailable(startsAt,endsAt));}
    @GetMapping("/api/quotes") public BookingService.Quote quote(@RequestParam long carId,@RequestParam Instant startsAt,@RequestParam Instant endsAt){return service.quote(carId,startsAt,endsAt);}
    @GetMapping("/api/bookings") public List<Map<String,Object>> list(){return service.list().stream().map(this::view).toList();}
    @GetMapping("/api/bookings/{id}") public Map<String,Object> detail(@PathVariable long id){var b=service.get(id);Map<String,Object> v=view(b);v.put("payments",service.payments(id));v.put("events",service.events(id));return v;}
    @PostMapping("/api/bookings") @PreAuthorize("hasRole('CUSTOMER')") public ResponseEntity<?> create(@RequestBody BookingService.Create r,@RequestHeader("Idempotency-Key") String key){return ResponseEntity.status(201).body(view(service.create(r,key)));}
    public record Action(String note){}
    @PostMapping("/api/bookings/{id}/actions/{action}") public Map<String,Object> action(@PathVariable long id,@PathVariable String action,@RequestBody(required=false) Action r){return view(service.action(id,action,r==null?null:r.note()));}
    @PostMapping("/api/bookings/{id}/payment-claims") @PreAuthorize("hasRole('CUSTOMER')") public PaymentClaim claim(@PathVariable long id,@RequestBody BookingService.Pay r){return service.claim(id,r,false);}
    @PostMapping("/api/bookings/{id}/cash-receipts") @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public PaymentClaim cash(@PathVariable long id,@RequestBody BookingService.Pay r){return service.claim(id,r,true);}
    public record Verify(boolean accepted,String note){}
    @PostMapping("/api/bookings/{id}/payments/{paymentId}/verify") @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public PaymentClaim verify(@PathVariable long id,@PathVariable long paymentId,@RequestBody Verify r){return service.verify(id,paymentId,r.accepted(),r.note());}
    @GetMapping("/api/bookings/{id}/payment-instructions") public Map<String,Object> instructions(@PathVariable long id){return service.instructions(id);}
    @GetMapping("/api/owner/payment-settings") @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public Map<String,Object> billing(){return service.billing(false,service.currentSubject());}
    @PutMapping("/api/owner/payment-settings") @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public Map<String,Object> billing(@RequestBody BookingService.BillingInput r){return service.saveBilling(r);}
}
