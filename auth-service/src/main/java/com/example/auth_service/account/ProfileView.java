package com.example.auth_service.account;

public record ProfileView(Long id,String username,String role,String displayNameMasked,
                          String phoneMasked,String emailMasked) {
    public static ProfileView of(Account account) {
        return new ProfileView(account.getId(),account.getUsername(),account.getRole(),
                Privacy.name(account.getDisplayName()),Privacy.phone(account.getPhone()),Privacy.email(account.getEmail()));
    }
}
