package com.example.carmanage_service.controller;
import com.example.carmanage_service.model.Car;
import com.example.carmanage_service.service.CarService;
import com.example.carmanage_service.dto.CarView;
import java.util.*;
import java.net.URI;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController @RequestMapping("/api/cars")
public class CarController {
    private final CarService cars;private final com.example.carmanage_service.location.ProvinceDirectory provinces;private final com.example.carmanage_service.catalog.CarCatalog catalog;
    public CarController(CarService cars,com.example.carmanage_service.location.ProvinceDirectory provinces,com.example.carmanage_service.catalog.CarCatalog catalog){this.cars=cars;this.provinces=provinces;this.catalog=catalog;}
    @GetMapping public List<CarView> list(@RequestParam(defaultValue="false") boolean mine,@RequestParam(required=false) String city,@RequestParam(required=false) String brand,@RequestParam(required=false) String model,@RequestParam(required=false) Integer seats,@RequestParam(required=false) java.math.BigDecimal maxPrice){
        return (mine?cars.findOwned():cars.findAll()).stream().filter(c->provinces.matches(c.getCity(),city)).filter(c->brand==null||catalog.matchesBrand(brand,c)).filter(c->model==null||catalog.matches(model,c)).filter(c->seats==null||c.getSeatCount()>=seats).filter(c->maxPrice==null||c.getPricePerDay().compareTo(maxPrice)<=0).map(c->CarView.of(c,cars.privateView(c))).toList();
    }
    @GetMapping("/{id}") public CarView get(@PathVariable long id){var c=cars.findById(id);return CarView.of(c,cars.privateView(c));}
    @PostMapping @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public ResponseEntity<CarView> create(@RequestBody Car r){var c=cars.create(r);return ResponseEntity.created(URI.create("/api/cars/"+c.getId())).body(CarView.of(c,true));}
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public CarView update(@PathVariable long id,@RequestBody Car r){return CarView.of(cars.update(id,r),true);}
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public Map<String,String> hide(@PathVariable long id){cars.delete(id);return Map.of("message","Đã lưu trữ xe; các chuyến đã đặt vẫn được giữ");}
    @PostMapping("/{id}/publish") @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CarView publish(@PathVariable long id){return CarView.of(cars.publish(id),true);}
    /** Backward-compatible URL: a submission now publishes immediately. */
    @PostMapping("/{id}/submit") @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CarView submit(@PathVariable long id){return publish(id);}
    @PostMapping("/{id}/review") @PreAuthorize("hasRole('ADMIN')")
    public void retiredReview(){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE,"Xe được đăng trực tiếp; không còn bước quản trị viên duyệt");}
}
