package com.example.rental_service.booking;
import jakarta.persistence.*;
@Entity @Table(name="booking_calendars") public class CalendarLock {@Id @Column(name="car_id") public Long carId;}
