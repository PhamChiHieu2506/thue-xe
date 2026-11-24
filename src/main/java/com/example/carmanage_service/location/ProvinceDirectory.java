package com.example.carmanage_service.location;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Official provincial names/codes; legacy aliases preserve searches over older addresses. */
@Component
public final class ProvinceDirectory {
    public record Province(String code, String name, String type, List<String> aliases) {
        public String getLabel() { return ("CITY".equals(type) ? "Thành phố " : "Tỉnh ") + name; }
    }
    private final List<Province> provinces;
    private final Map<String, Province> lookup = new HashMap<>();
    public ProvinceDirectory() throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("vietnam-provinces.tsv").getInputStream(), StandardCharsets.UTF_8))) {
            provinces = reader.lines().filter(s -> !s.isBlank() && !s.startsWith("#")).map(line -> {
                String[] f = line.split("\t", -1);
                if (f.length != 4) throw new IllegalStateException("Invalid province row");
                return new Province(f[0], f[1], f[2], f[3].isBlank() ? List.of() : List.of(f[3].split("\\|")));
            }).toList();
        }
        if (provinces.size() != 34 || provinces.stream().map(Province::code).distinct().count() != 34)
            throw new IllegalStateException("Province directory must contain 34 unique codes");
        for (Province p : provinces) {
            register(p.code(), p); register(p.name(), p); register(p.getLabel(), p);
            for (String alias : p.aliases()) register(alias, p);
        }
    }
    private void register(String name, Province p) {
        Province old = lookup.putIfAbsent(normalize(name), p);
        if (old != null && !old.code().equals(p.code())) throw new IllegalStateException("Ambiguous province alias");
    }
    public List<Province> all() { return provinces; }
    public Optional<Province> resolve(String value) { return Optional.ofNullable(lookup.get(normalize(value))); }
    public String canonical(String value) {
        if (value == null || value.isBlank()) return value;
        return resolve(value).map(Province::name).orElseThrow(() -> new IllegalArgumentException("Chọn tỉnh/thành phố trong danh sách"));
    }
    public boolean matches(String stored, String selected) {
        if (selected == null || selected.isBlank()) return true;
        var expected = resolve(selected); var actual = resolve(stored);
        if (expected.isPresent() && actual.isPresent()) return expected.get().code().equals(actual.get().code());
        return stored != null && normalize(stored).contains(normalize(selected));
    }
    private static String normalize(String value) {
        if (value == null) return "";
        String n = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
        return n.replaceFirst("^(?:tinh|thanh pho|tp\\.?)\\s*", "").replaceAll("[^a-z0-9]", "");
    }
}
