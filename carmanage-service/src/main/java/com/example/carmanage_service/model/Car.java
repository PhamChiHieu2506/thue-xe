package com.example.carmanage_service.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="owner_id", length=64)
    private String ownerId;

    @Column(
            name = "license_plate",
            nullable = false,
            unique = true,
            length = 20
    )
    private String licensePlate;

    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    @Column(
            name = "brand",
            nullable = false,
            length = 100
    )
    private String brand;

    @Column(name = "seat_count", nullable = false)
    private Integer seatCount;

    @Column(
            name = "price_per_day",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal pricePerDay;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name="catalog_key", length=64)
    private String catalogKey;
    @Column(name="source_url", length=500)
    private String sourceUrl;
    @Column(name="source_verified_at", length=32)
    private String sourceVerifiedAt;
    // Nullable for existing PostgreSQL rows during an additive schema update.
    @Column(name="demo_vehicle")
    private Boolean legacySample;

    @jakarta.persistence.Transient
    @com.fasterxml.jackson.annotation.JsonProperty(access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public String getImageUrl() {
        return photoIds!=null&&!photoIds.isEmpty()?"/media/"+photoIds.getFirst():com.example.carmanage_service.catalog.CarImages.forCar(catalogKey,name,brand);
    }

    public String getCatalogKey() { return catalogKey; }
    public void setCatalogKey(String value) { catalogKey = value; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String value) { sourceUrl = value; }
    public String getSourceVerifiedAt() { return sourceVerifiedAt; }
    public void setSourceVerifiedAt(String value) { sourceVerifiedAt = value; }
    public boolean isLegacySample() { return Boolean.TRUE.equals(legacySample); }
    public void setLegacySample(boolean value) { legacySample = value; }

    @Column(name="listing_status",length=30) private String listingStatus;
    @Column(length=100) private String city;
    @Column(length=150) private String district;
    @Column(name="pickup_address",length=500) private String pickupAddress;
    @Column(length=3000) private String description;
    @Column(name="model_year") private Integer modelYear;
    @Column(length=20) private String transmission;
    @Column(name="fuel_type",length=20) private String fuelType;
    @Column(name="review_note",length=500) private String reviewNote;
    @jakarta.persistence.ElementCollection(fetch=jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.CollectionTable(name="car_photo_links",joinColumns=@jakarta.persistence.JoinColumn(name="car_id"))
    @Column(name="photo_id",length=32)
    private java.util.List<String> photoIds=new java.util.ArrayList<>();
    public String getListingStatus(){return listingStatus==null?"DRAFT":listingStatus;}
    public void setListingStatus(String v){listingStatus=v;}
    public String getCity(){return city;}public void setCity(String v){city=v;}
    public String getDistrict(){return district;}public void setDistrict(String v){district=v;}
    public String getPickupAddress(){return pickupAddress;}public void setPickupAddress(String v){pickupAddress=v;}
    public String getDescription(){return description;}public void setDescription(String v){description=v;}
    public Integer getModelYear(){return modelYear;}public void setModelYear(Integer v){modelYear=v;}
    public String getTransmission(){return transmission;}public void setTransmission(String v){transmission=v;}
    public String getFuelType(){return fuelType;}public void setFuelType(String v){fuelType=v;}
    public String getReviewNote(){return reviewNote;}public void setReviewNote(String v){reviewNote=v;}
    public java.util.List<String> getPhotoIds(){return photoIds;}
    public void setPhotoIds(java.util.List<String> v){photoIds=new java.util.ArrayList<>(v==null?java.util.List.of():v);}
    public boolean isPublished(){return !isLegacySample()&&"PUBLISHED".equals(getListingStatus());}

    public Car() {
    }

    public Car(Long id, String licensePlate, String name, String brand,
               Integer seatCount, BigDecimal pricePerDay, String status) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.name = name;
        this.brand = brand;
        this.seatCount = seatCount;
        this.pricePerDay = pricePerDay;
        this.status = status;
    }

    public Car(Car other) {
        this(other.id, other.licensePlate, other.name, other.brand,
                other.seatCount, other.pricePerDay, other.status);
        this.ownerId = other.ownerId;
        this.catalogKey = other.catalogKey;
        this.sourceUrl = other.sourceUrl;
        this.sourceVerifiedAt = other.sourceVerifiedAt;
        this.legacySample = other.legacySample;
        this.listingStatus=other.listingStatus;this.city=other.city;this.district=other.district;
        this.pickupAddress=other.pickupAddress;this.description=other.description;this.modelYear=other.modelYear;
        this.transmission=other.transmission;this.fuelType=other.fuelType;this.reviewNote=other.reviewNote;
        setPhotoIds(other.photoIds);
    }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public Integer getSeatCount() { return seatCount; }
    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public BigDecimal getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}