package com.example.auth_service.account;
import java.util.List;
public final class RolePermissions {
    private RolePermissions() {}
    public static List<String> forRole(String role) {
        return switch(role) {
            case "ADMIN" -> List.of("car:read", "car:write:any", "quote:read");
            case "OWNER" -> List.of("car:read", "car:write:own", "quote:read");
            case "CUSTOMER" -> List.of("car:read", "quote:read");
            default -> List.of();
        };
    }
}
