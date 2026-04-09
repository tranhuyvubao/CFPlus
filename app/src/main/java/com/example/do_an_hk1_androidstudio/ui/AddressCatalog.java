package com.example.do_an_hk1_androidstudio.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AddressCatalog {

    private static final List<String> COUNTRIES = Arrays.asList("Việt Nam", "Singapore", "Thailand");
    private static final Map<String, List<String>> PROVINCES = new LinkedHashMap<>();
    private static final Map<String, List<String>> DISTRICTS = new LinkedHashMap<>();
    private static final Map<String, List<String>> WARDS = new LinkedHashMap<>();

    static {
        PROVINCES.put("Việt Nam", Arrays.asList("TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ"));
        PROVINCES.put("Singapore", Arrays.asList("Central Region", "East Region"));
        PROVINCES.put("Thailand", Arrays.asList("Bangkok", "Chiang Mai"));

        DISTRICTS.put(key("Việt Nam", "TP. Hồ Chí Minh"), Arrays.asList("Quận 1", "Quận 7", "TP. Thủ Đức"));
        DISTRICTS.put(key("Việt Nam", "Hà Nội"), Arrays.asList("Ba Đình", "Cầu Giấy", "Hoàng Mai"));
        DISTRICTS.put(key("Việt Nam", "Đà Nẵng"), Arrays.asList("Hải Châu", "Sơn Trà", "Thanh Khê"));
        DISTRICTS.put(key("Việt Nam", "Cần Thơ"), Arrays.asList("Ninh Kiều", "Bình Thủy", "Cái Răng"));
        DISTRICTS.put(key("Singapore", "Central Region"), Arrays.asList("Downtown Core", "Orchard", "Bukit Merah"));
        DISTRICTS.put(key("Singapore", "East Region"), Arrays.asList("Bedok", "Tampines", "Pasir Ris"));
        DISTRICTS.put(key("Thailand", "Bangkok"), Arrays.asList("Pathum Wan", "Bang Kapi", "Sathon"));
        DISTRICTS.put(key("Thailand", "Chiang Mai"), Arrays.asList("Mueang Chiang Mai", "Hang Dong"));

        WARDS.put(key("Việt Nam", "TP. Hồ Chí Minh", "Quận 1"), Arrays.asList("Bến Nghé", "Bến Thành", "Cầu Ông Lãnh"));
        WARDS.put(key("Việt Nam", "TP. Hồ Chí Minh", "Quận 7"), Arrays.asList("Tân Phong", "Tân Quy", "Phú Mỹ"));
        WARDS.put(key("Việt Nam", "TP. Hồ Chí Minh", "TP. Thủ Đức"), Arrays.asList("Hiệp Bình Chánh", "Linh Trung", "An Phú"));
        WARDS.put(key("Việt Nam", "Hà Nội", "Ba Đình"), Arrays.asList("Điện Biên", "Kim Mã", "Ngọc Hà"));
        WARDS.put(key("Việt Nam", "Hà Nội", "Cầu Giấy"), Arrays.asList("Dịch Vọng", "Nghĩa Tân", "Mai Dịch"));
        WARDS.put(key("Việt Nam", "Hà Nội", "Hoàng Mai"), Arrays.asList("Định Công", "Hoàng Liệt", "Tương Mai"));
        WARDS.put(key("Việt Nam", "Đà Nẵng", "Hải Châu"), Arrays.asList("Hải Châu I", "Hải Châu II", "Bình Hiên"));
        WARDS.put(key("Việt Nam", "Đà Nẵng", "Sơn Trà"), Arrays.asList("An Hải Bắc", "Phước Mỹ", "Mân Thái"));
        WARDS.put(key("Việt Nam", "Đà Nẵng", "Thanh Khê"), Arrays.asList("Tam Thuận", "Thanh Khê Tây", "Xuân Hà"));
        WARDS.put(key("Việt Nam", "Cần Thơ", "Ninh Kiều"), Arrays.asList("An Khánh", "An Bình", "Tân An"));
        WARDS.put(key("Việt Nam", "Cần Thơ", "Bình Thủy"), Arrays.asList("Bình Thủy", "Long Hòa", "An Thới"));
        WARDS.put(key("Việt Nam", "Cần Thơ", "Cái Răng"), Arrays.asList("Hưng Phú", "Lê Bình", "Ba Láng"));
        WARDS.put(key("Singapore", "Central Region", "Downtown Core"), Arrays.asList("Marina Centre", "Clifford Pier"));
        WARDS.put(key("Singapore", "Central Region", "Orchard"), Arrays.asList("Somerset", "Paterson"));
        WARDS.put(key("Singapore", "East Region", "Bedok"), Arrays.asList("Bedok North", "Bedok South"));
        WARDS.put(key("Thailand", "Bangkok", "Pathum Wan"), Arrays.asList("Lumphini", "Maha Phruettharam"));
        WARDS.put(key("Thailand", "Bangkok", "Bang Kapi"), Arrays.asList("Hua Mak", "Khlong Chan"));
    }

    private AddressCatalog() {
    }

    public static List<String> getCountries() {
        return new ArrayList<>(COUNTRIES);
    }

    public static List<String> getProvinces(String country) {
        return new ArrayList<>(PROVINCES.getOrDefault(country, new ArrayList<>()));
    }

    public static List<String> getDistricts(String country, String province) {
        return new ArrayList<>(DISTRICTS.getOrDefault(key(country, province), new ArrayList<>()));
    }

    public static List<String> getWards(String country, String province, String district) {
        return new ArrayList<>(WARDS.getOrDefault(key(country, province, district), new ArrayList<>()));
    }

    private static String key(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part == null ? "" : part).append("::");
        }
        return builder.toString();
    }
}
