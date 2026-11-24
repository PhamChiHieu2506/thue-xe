package com.example.auth_service.account;

/** Mask before serializing; never send contact values to a browser to hide with CSS. */
public final class Privacy {
    private Privacy() {}
    public static String phone(String value) {
        if(value==null||value.isBlank())return null;
        String digits=value.replaceAll("[^0-9]","");
        return digits.length()<=3?"***":"*".repeat(digits.length()-3)+digits.substring(digits.length()-3);
    }
    public static String email(String value) {
        if(value==null||value.isBlank())return null;
        int at=value.indexOf('@');
        return at<1?"***":value.substring(0,1)+"***"+value.substring(at);
    }
    public static String name(String value) {
        if(value==null||value.isBlank())return null;
        return java.util.Arrays.stream(value.trim().split("\\s+"))
                .map(word->word.substring(0,word.offsetByCodePoints(0,1))+"***")
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
