package com.example.rental_service.booking;
import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
@Entity @Table(name="bookings",indexes={@Index(name="booking_car_calendar_idx",columnList="car_id,starts_at,ends_at"),@Index(name="booking_customer_idx",columnList="customer_id"),@Index(name="booking_owner_idx",columnList="owner_id")},uniqueConstraints=@UniqueConstraint(columnNames={"customer_id","request_key"}))
public class Booking {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="car_id",nullable=false) public Long carId;
    @Column(name="customer_id",nullable=false,length=64) public String customerId;
    @Column(name="owner_id",nullable=false,length=64) public String ownerId;
    @Column(name="request_key",nullable=false,length=80) public String requestKey;
    @Column(nullable=false,length=150) public String carName;
    @Column(length=500) public String imageUrl;
    @Column(length=200) public String pickupArea;
    @Column(length=500) public String handoverAddress;
    @Column(name="starts_at",nullable=false) public Instant startsAt;
    @Column(name="ends_at",nullable=false) public Instant endsAt;
    @Column(nullable=false) public int days;
    @Column(nullable=false,precision=18,scale=2) public BigDecimal pricePerDay;
    @Column(nullable=false,precision=18,scale=2) public BigDecimal totalPrice;
    @Column(nullable=false,precision=18,scale=2) public BigDecimal deposit;
    @Column(nullable=false,precision=18,scale=2) public BigDecimal paid=BigDecimal.ZERO;
    @Column(nullable=false,length=30) public String status="PENDING";
    @Column(length=1000) public String message;
    @Column(length=500) public String reason;
    public Instant createdAt=Instant.now();public Instant updatedAt=Instant.now();
    public Instant paymentDeadline;public Instant actualStart;public Instant actualEnd;
    @Column(precision=18,scale=2) public BigDecimal refundDue=BigDecimal.ZERO;
    public Instant refundedAt;
    @Column(length=100) public String refundReference;
}
