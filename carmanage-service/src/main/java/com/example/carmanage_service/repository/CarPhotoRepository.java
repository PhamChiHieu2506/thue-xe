package com.example.carmanage_service.repository;
import com.example.carmanage_service.model.CarPhoto;
public interface CarPhotoRepository extends org.springframework.data.jpa.repository.JpaRepository<CarPhoto,String>{long countByOwnerId(String ownerId);}
