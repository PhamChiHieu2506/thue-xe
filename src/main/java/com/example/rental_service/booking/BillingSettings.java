package com.example.rental_service.booking;
import jakarta.persistence.*;
@Entity @Table(name="owner_billing") public class BillingSettings {
    @Id @Column(length=64) public String ownerId;
    @Column(length=20) public String method="CASH";
    @Column(length=100) public String bankName;
    @Column(length=40) public String accountNumber;
    @Column(length=150) public String accountHolder;
}
