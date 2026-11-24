package com.example.rental_service.booking;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="booking_events")
public class BookingEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="booking_id",nullable=false) public Long bookingId;
    @Column(nullable=false,length=50) public String action;
    @Column(nullable=false,length=64) public String actor;
    @Column(length=1000) public String note;
    public Instant createdAt=Instant.now();
    public BookingEvent(){}public BookingEvent(long id,String action,String actor,String note){bookingId=id;this.action=action;this.actor=actor;this.note=note;}
}
