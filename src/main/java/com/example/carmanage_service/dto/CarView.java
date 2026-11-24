package com.example.carmanage_service.dto;
import com.example.carmanage_service.model.Car;
import java.util.*;
import java.math.BigDecimal;
public record CarView(Long id,String ownerId,String licensePlate,String name,String brand,Integer seatCount,
        BigDecimal pricePerDay,String status,String listingStatus,boolean published,String city,String district,
        String pickupAddress,String description,Integer modelYear,String transmission,String fuelType,
        String reviewNote,List<String> photoIds,List<String> imageUrls,String imageUrl,String modelId) {
    public static CarView of(Car c,boolean own){return new CarView(c.getId(),c.getOwnerId(),own?c.getLicensePlate():"***"+c.getLicensePlate().substring(Math.max(0,c.getLicensePlate().length()-3)),c.getName(),c.getBrand(),c.getSeatCount(),c.getPricePerDay(),c.getStatus(),c.getListingStatus(),c.isPublished(),c.getCity(),c.getDistrict(),own?c.getPickupAddress():null,c.getDescription(),c.getModelYear(),c.getTransmission(),c.getFuelType(),own?c.getReviewNote():null,List.copyOf(c.getPhotoIds()),c.getPhotoIds().isEmpty()?List.of(c.getImageUrl()):c.getPhotoIds().stream().map(id->"/media/"+id).toList(),c.getImageUrl(),com.example.carmanage_service.catalog.CarImages.keyForCar(null,c.getName(),c.getBrand()));}
}
