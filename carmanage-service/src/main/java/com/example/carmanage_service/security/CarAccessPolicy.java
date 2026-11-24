package com.example.carmanage_service.security;
import com.example.carmanage_service.model.Car;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
@Component
public class CarAccessPolicy {
    public String subject() { return SecurityContextHolder.getContext().getAuthentication().getName(); }
    public void requireWrite(Car car) {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        boolean admin=auth.getAuthorities().stream().anyMatch(a->"car:write:any".equals(a.getAuthority()));
        if(!admin && !Objects.equals(car.getOwnerId(),subject())) {
            throw new AccessDeniedException("Ban khong so huu xe nay");
        }
    }
}
