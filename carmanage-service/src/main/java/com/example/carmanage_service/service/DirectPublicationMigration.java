package com.example.carmanage_service.service;

import com.example.carmanage_service.location.ProvinceDirectory;
import com.example.carmanage_service.repository.CarRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Completes already-submitted legacy listings; drafts/archived/sample rows stay private. */
@Component
public class DirectPublicationMigration implements CommandLineRunner {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(DirectPublicationMigration.class);
    private final CarRepository cars;private final ProvinceDirectory provinces;private final PhotoService photos;
    public DirectPublicationMigration(CarRepository cars,ProvinceDirectory provinces,PhotoService photos){this.cars=cars;this.provinces=provinces;this.photos=photos;}
    @Override @Transactional public void run(String... args){
        int changed=0;
        for(var car:cars.findByListingStatus("PENDING_REVIEW")){
            if(car.isLegacySample())continue;
            try{
                PublicationRules.require(car,provinces);
                photos.requireOwned(car.getPhotoIds(),car.getOwnerId(),false);
                changed+=cars.publishPreviouslySubmitted(car.getId());
            }catch(IllegalArgumentException|ResponseStatusException e){log.warn("Vehicle {} still needs information before publication",car.getId());}
        }
        if(changed>0)log.info("Published {} previously submitted vehicles without an approval queue",changed);
    }
}
