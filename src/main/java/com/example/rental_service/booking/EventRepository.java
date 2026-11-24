package com.example.rental_service.booking;
public interface EventRepository extends org.springframework.data.jpa.repository.JpaRepository<BookingEvent,Long>{java.util.List<BookingEvent> findByBookingIdOrderByCreatedAtAsc(Long id);}
