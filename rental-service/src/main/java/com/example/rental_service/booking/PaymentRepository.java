package com.example.rental_service.booking;
import java.util.*;
public interface PaymentRepository extends org.springframework.data.jpa.repository.JpaRepository<PaymentClaim,Long>{List<PaymentClaim> findByBookingIdOrderByCreatedAtAsc(Long id);Optional<PaymentClaim> findByBookingIdAndReference(Long id,String reference);}
