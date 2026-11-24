package com.example.rental_service.dto;

import java.math.BigDecimal;

public class CarResponse {

    private Long id;
    private String licensePlate;
    private String name;
    private String brand;
    private Integer seatCount;
    private BigDecimal pricePerDay;
    private String status;

    private String ownerId;private Boolean published;private String imageUrl;
    public String getOwnerId(){return ownerId;}public void setOwnerId(String v){ownerId=v;}
    public Boolean getPublished(){return published;}public void setPublished(Boolean v){published=v;}
    public String getImageUrl(){return imageUrl;}public void setImageUrl(String v){imageUrl=v;}
    public CarResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}