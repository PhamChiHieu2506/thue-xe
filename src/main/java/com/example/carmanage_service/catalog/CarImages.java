package com.example.carmanage_service.catalog;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/** Only bundled, allowlisted image paths are returned, including for legacy car records. */
public final class CarImages {
    public static final String FALLBACK="/images/fleet-garage.png";
    private static final Properties PATHS=load();
    private CarImages() {}
    private static Properties load() {
        Properties paths=new Properties();
        try(var input=CarImages.class.getResourceAsStream("/car-image-paths.properties")) {
            if(input==null)throw new IllegalStateException("Missing car image paths");
            paths.load(input);
        } catch(IOException e){throw new IllegalStateException("Cannot read car image paths",e);}
        for(String key:paths.stringPropertyNames()) {
            String value=paths.getProperty(key);
            if(!value.matches("/images/cars/[a-z0-9-]+\\.(png|jpg|webp)"))throw new IllegalStateException("Invalid bundled car image path");
        }
        return paths;
    }
    public static String forKey(String key){return key==null?FALLBACK:PATHS.getProperty(key,FALLBACK);}
    public static String forCar(String key,String name,String brand) { return forKey(keyForCar(key,name,brand)); }
    public static String keyForCar(String key,String name,String brand) {
        if(key!=null&&PATHS.containsKey(key))return key;
        String n=(String.valueOf(brand)+" "+String.valueOf(name)).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
        String[][] aliases={
            {"corollacross","toyota-corolla-cross"},{"innovacross","toyota-innova-hev"},{"veloz","toyota-veloz"},{"vios","toyota-vios"},
            {"vf3","vinfast-vf3"},{"vf5","vinfast-vf5"},{"vf6","vinfast-vf6"},{"vf7","vinfast-vf7"},
            {"stargazer","hyundai-stargazer"},{"tucson","hyundai-tucson"},{"creta","hyundai-creta"},{"accent","hyundai-accent"},
            {"carnival","kia-carnival"},{"carens","kia-carens"},{"seltos","kia-seltos"},{"sonet","kia-sonet"},
            {"cx5","mazda-cx5"},{"cx8","mazda-cx8"},{"mazda2","mazda-2"},{"mazda3","mazda-3"},{"g63","mercedes-g63"}};
        for(String[] alias:aliases)if(n.contains(alias[0]))return alias[1];
        return null;
    }
}
