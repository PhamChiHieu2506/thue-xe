package com.example.carmanage_service.model;
import jakarta.persistence.*;
@Entity @Table(name="car_photos")
public class CarPhoto {
    @Id @Column(length=32) public String id;
    @Column(name="owner_id",nullable=false,length=64) public String ownerId;
    @Column(nullable=false,length=80) public String filename;
    @Column(nullable=false,length=40) public String contentType;
    public CarPhoto(){}
    public CarPhoto(String id,String owner,String filename,String type){this.id=id;this.ownerId=owner;this.filename=filename;this.contentType=type;}
}
