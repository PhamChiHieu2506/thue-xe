package com.example.auth_service.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ProfileService {
    private final AccountRepository accounts;
    public ProfileService(AccountRepository accounts){this.accounts=accounts;}
    public record Update(String displayName,String phone,String email) {}
    @Transactional(readOnly=true)
    public ProfileView view(String subject){return ProfileView.of(account(subject));}
    @Transactional
    public ProfileView update(String subject,Update change) {
        Account account=account(subject);
        if(change.displayName()!=null) {
            String name=change.displayName().trim();
            if(name.length()>100||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Ho ten khong hop le");
            account.setDisplayName(name.isEmpty()?null:name);
        }
        if(change.phone()!=null) {
            String phone=change.phone().replaceAll("[\\s()\\-]","");
            if(!phone.isEmpty()&&!phone.matches("\\+?[0-9]{7,15}"))throw new IllegalArgumentException("So dien thoai can tu 7 den 15 chu so");
            account.setPhone(phone.isEmpty()?null:phone);
        }
        if(change.email()!=null) {
            String email=change.email().trim();
            if(email.length()>254||(!email.isEmpty()&&!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))throw new IllegalArgumentException("Email khong hop le");
            account.setEmail(email.isEmpty()?null:email);
        }
        return ProfileView.of(accounts.saveAndFlush(account));
    }
    private Account account(String subject) {
        long id;
        try{id=Long.parseLong(subject);}catch(NumberFormatException e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
        return accounts.findById(id).filter(Account::isEnabled)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
