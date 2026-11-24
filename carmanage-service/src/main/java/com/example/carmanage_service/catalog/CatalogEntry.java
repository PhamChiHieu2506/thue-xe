package com.example.carmanage_service.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Manufacturer model data plus separately configured storefront display settings. */
public record CatalogEntry(String id, String brand, String name, int seatCount,
        String sourceName, String sourceUrl, String verifiedAt,
        BigDecimal displayPricePerDay, String displayCity) {
    public CatalogEntry {
        if (displayPricePerDay == null || displayPricePerDay.signum() <= 0)
            throw new IllegalArgumentException("Catalog display price must be positive");
        displayPricePerDay = displayPricePerDay.setScale(2, RoundingMode.UNNECESSARY);
        if (displayPricePerDay.precision() > 18 || displayCity == null || displayCity.isBlank())
            throw new IllegalArgumentException("Invalid catalog display settings");
    }
    public String getImageUrl() { return CarImages.forKey(id); }
}
