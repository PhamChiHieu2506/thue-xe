package com.example.rental_service.booking;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="payment_claims",uniqueConstraints=@UniqueConstraint(columnNames={"booking_id","reference"}))
public class PaymentClaim {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="booking_id",nullable=false) public Long bookingId;
    @Column(nullable=false,precision=18,scale=2) public BigDecimal amount;
    @Column(nullable=false,length=100) public String reference;
    @Column(nullable=false,length=20) public String status="PENDING";
    @Column(nullable=false,length=20) public String method="BANK_TRANSFER";
    @Column(length=500) public String note;
    public Instant createdAt=Instant.now();public Instant confirmedAt;
}
