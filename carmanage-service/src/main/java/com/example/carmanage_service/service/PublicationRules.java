package com.example.carmanage_service.service;

import com.example.carmanage_service.model.Car;
import com.example.carmanage_service.catalog.CarImages;
import com.example.carmanage_service.location.ProvinceDirectory;
import java.time.Year;
import java.util.Set;

/** Checks completeness, not an administrative approval. No authentication context required. */
public final class PublicationRules {
    private PublicationRules() {}
    public static void require(Car c, ProvinceDirectory provinces) {
        required(c.getOwnerId(), "Thiếu chủ xe");
        required(c.getLicensePlate(), "Cần nhập biển số");
        required(c.getName(), "Cần nhập tên xe"); required(c.getBrand(), "Cần nhập hãng xe");
        if (c.getPricePerDay() == null || c.getPricePerDay().signum() <= 0)
            throw new IllegalArgumentException("Cần nhập giá thuê lớn hơn 0");
        if (c.getSeatCount() == null || c.getSeatCount() < 2 || c.getSeatCount() > 16)
            throw new IllegalArgumentException("Số chỗ từ 2 đến 16");
        required(c.getCity(), "Chọn tỉnh/thành phố nhận xe");
        if (provinces.resolve(c.getCity()).isEmpty()) throw new IllegalArgumentException("Chọn tỉnh/thành phố trong danh sách");
        required(c.getDistrict(), "Cần nhập phường/xã hoặc khu vực nhận xe");
        required(c.getPickupAddress(), "Cần nhập địa chỉ nhận xe");
        required(c.getDescription(), "Cần nhập mô tả và điều kiện thuê");
        if (c.getModelYear() == null || c.getModelYear() < 1990 || c.getModelYear() > Year.now().getValue() + 1)
            throw new IllegalArgumentException("Cần nhập năm sản xuất hợp lệ");
        if (!Set.of("AVAILABLE", "RENTED", "MAINTENANCE", "INACTIVE").contains(String.valueOf(c.getStatus())))
            throw new IllegalArgumentException("Trạng thái xe không hợp lệ");
        if (c.getPhotoIds().isEmpty() && CarImages.keyForCar(null, c.getName(), c.getBrand()) == null)
            throw new IllegalArgumentException("Chọn dòng xe có ảnh hoặc tải ít nhất một ảnh xe");
    }
    private static void required(String text, String message) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException(message);
    }
}
