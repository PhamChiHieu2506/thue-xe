package com.example.rental_service.booking;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface BookingRepository extends JpaRepository<Booking,Long> {
    Optional<Booking> findByCustomerIdAndRequestKey(String customer,String key);
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String id);
    List<Booking> findByOwnerIdOrderByCreatedAtDesc(String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from Booking b where b.id=:id") Optional<Booking> locked(@Param("id") Long id);
    @Query("select b from Booking b where b.startsAt < :end and (b.status='IN_PROGRESS' or (b.endsAt > :start and (b.status='CONFIRMED' or (b.status='APPROVED' and b.paymentDeadline > :now))))")
    List<Booking> blocking(@Param("start") Instant start,@Param("end") Instant end,@Param("now") Instant now);
    @Query("select b.id from Booking b where b.status='APPROVED' and b.paymentDeadline < :now") List<Long> expired(@Param("now") Instant now);
}
