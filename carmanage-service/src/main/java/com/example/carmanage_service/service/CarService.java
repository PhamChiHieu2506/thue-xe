package com.example.carmanage_service.service;
import com.example.carmanage_service.model.Car;
import com.example.carmanage_service.repository.CarRepository;
import com.example.carmanage_service.security.CarAccessPolicy;
import java.util.*;
import java.math.*;
import java.time.Year;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service @Transactional(readOnly=true)
public class CarService {
    private final CarRepository repository;private final CarAccessPolicy access;private final PhotoService photos;private final com.example.carmanage_service.location.ProvinceDirectory provinces;
    public CarService(CarRepository repository,CarAccessPolicy access,PhotoService photos,com.example.carmanage_service.location.ProvinceDirectory provinces){this.repository=repository;this.access=access;this.photos=photos;this.provinces=provinces;}
    public boolean admin(){return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->"ROLE_ADMIN".equals(a.getAuthority()));}
    public boolean privateView(Car c){return admin()||Objects.equals(c.getOwnerId(),access.subject());}
    private boolean visible(Car c){return !c.isLegacySample()&&!"ARCHIVED".equals(c.getListingStatus());}
    public List<Car> findAll(){return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream().filter(this::visible).filter(c->admin()||c.isPublished()).map(Car::new).toList();}
    public List<Car> findOwned(){if(!SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->Set.of("ROLE_OWNER","ROLE_ADMIN").contains(a.getAuthority())))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return repository.findByOwnerIdOrderByIdAsc(access.subject()).stream().filter(this::visible).map(Car::new).toList();}
    public Car stored(long id){return repository.findById(id).filter(this::visible).orElseThrow(()->new NoSuchElementException("Không tìm thấy xe"));}
    public Car findById(long id){Car c=stored(id);if(!c.isPublished()&&!privateView(c))throw new NoSuchElementException("Không tìm thấy xe");return new Car(c);}
    @Transactional public Car create(Car request){
        Car c=validated(request);c.setId(null);c.setOwnerId(access.subject());
        c.setLegacySample(false);clearProvenance(c);c.setListingStatus("PUBLISHED");c.setReviewNote(null);
        PublicationRules.require(c,provinces);return save(c);
    }
    @Transactional public Car update(long id,Car request){
        Car old=stored(id);access.requireWrite(old);Car c=validated(request);c.setId(old.getId());c.setOwnerId(old.getOwnerId());
        c.setLegacySample(false);clearProvenance(c);c.setListingStatus("PUBLISHED");c.setReviewNote(null);
        PublicationRules.require(c,provinces);return save(c);
    }
    @Transactional public Car publish(long id){
        Car c=stored(id);access.requireWrite(c);PublicationRules.require(c,provinces);
        photos.requireOwned(c.getPhotoIds(),c.getOwnerId(),admin());
        c.setListingStatus("PUBLISHED");c.setReviewNote(null);return save(c);
    }
    private static void clearProvenance(Car c){c.setCatalogKey(null);c.setSourceUrl(null);c.setSourceVerifiedAt(null);}
    @Transactional public void delete(long id){Car c=stored(id);access.requireWrite(c);c.setListingStatus("ARCHIVED");repository.save(c);}
    private Car validated(Car r){
        if(r==null)throw new IllegalArgumentException("Thiếu thông tin xe");Car c=new Car(r);
        c.setLicensePlate(required(c.getLicensePlate(),20).toUpperCase(Locale.ROOT));c.setName(required(c.getName(),150));c.setBrand(required(c.getBrand(),100));
        if(c.getSeatCount()==null||c.getSeatCount()<2||c.getSeatCount()>16)throw new IllegalArgumentException("Số chỗ từ 2 đến 16");
        if(c.getPricePerDay()==null||c.getPricePerDay().signum()<=0)throw new IllegalArgumentException("Giá thuê cần lớn hơn 0");
        try{c.setPricePerDay(c.getPricePerDay().setScale(2,RoundingMode.UNNECESSARY));}catch(ArithmeticException e){throw new IllegalArgumentException("Giá có tối đa 2 số thập phân");}
        if(c.getPricePerDay().precision()>18)throw new IllegalArgumentException("Giá thuê vượt giới hạn");
        if(!Set.of("AVAILABLE","RENTED","MAINTENANCE","INACTIVE").contains(String.valueOf(c.getStatus())))throw new IllegalArgumentException("Trạng thái xe không hợp lệ");
        c.setCity(provinces.canonical(text(c.getCity(),100)));c.setDistrict(text(c.getDistrict(),150));c.setPickupAddress(text(c.getPickupAddress(),500));c.setDescription(text(c.getDescription(),3000));
        if(c.getModelYear()!=null&&(c.getModelYear()<1990||c.getModelYear()>Year.now().getValue()+1))throw new IllegalArgumentException("Năm sản xuất không hợp lệ");
        c.setTransmission(c.getTransmission()==null?"AUTOMATIC":c.getTransmission());c.setFuelType(c.getFuelType()==null?"PETROL":c.getFuelType());
        if(!Set.of("AUTOMATIC","MANUAL").contains(c.getTransmission())||!Set.of("PETROL","DIESEL","ELECTRIC","HYBRID").contains(c.getFuelType()))throw new IllegalArgumentException("Loại hộp số hoặc nhiên liệu không hợp lệ");
        photos.requireOwned(c.getPhotoIds(),access.subject(),admin());return c;
    }
    private Car save(Car c){
        if(c.getId()==null?repository.existsByLicensePlateIgnoreCase(c.getLicensePlate()):repository.existsByLicensePlateIgnoreCaseAndIdNot(c.getLicensePlate(),c.getId()))throw new IllegalStateException("Biển số đã được sử dụng");
        try{return new Car(repository.saveAndFlush(c));}catch(org.springframework.dao.DataIntegrityViolationException e){throw new IllegalStateException("Thông tin xe bị trùng hoặc không hợp lệ");}
    }
    private static boolean blank(String v){return v==null||v.isBlank();}
    private static String text(String v,int max){if(v==null)return null;String t=v.trim();if(t.length()>max||t.chars().anyMatch(ch->Character.isISOControl(ch)&&ch!='\n'))throw new IllegalArgumentException("Nội dung quá dài hoặc có ký tự không hợp lệ");return t;}
    private static String required(String v,int max){String t=text(v,max);if(blank(t))throw new IllegalArgumentException("Cần nhập đủ các trường bắt buộc");return t;}
}
