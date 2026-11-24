package com.example.carmanage_service.repository;

import com.example.carmanage_service.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarRepository extends JpaRepository<Car, Long> {
    java.util.List<Car> findByOwnerIdOrderByIdAsc(String ownerId);
    java.util.List<Car> findByListingStatus(String listingStatus);
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically=true,flushAutomatically=true)
    @org.springframework.data.jpa.repository.Query("update Car c set c.listingStatus='PUBLISHED',c.reviewNote=null where c.id=:id and c.listingStatus='PENDING_REVIEW'")
    int publishPreviouslySubmitted(@org.springframework.data.repository.query.Param("id") Long id);


    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    boolean existsByLicensePlateIgnoreCaseAndIdNot(
            String licensePlate, Long id);
}