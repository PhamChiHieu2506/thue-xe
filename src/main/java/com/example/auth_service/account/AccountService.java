package com.example.auth_service.account;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class AccountService {
    private final AccountRepository accounts;private final PasswordEncoder passwords;
    public AccountService(AccountRepository accounts,PasswordEncoder passwords){this.accounts=accounts;this.passwords=passwords;}
    public record Registration(String username,String password,String displayName,String phone,String email,String role){}
    public static void validatePassword(String value){if(value==null||value.length()<6||value.getBytes(StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("Mật khẩu cần ít nhất 6 ký tự, tối đa 72 byte");}
    @Transactional public ProfileView register(Registration request){
        String username=request.username()==null?"":request.username().trim().toLowerCase(Locale.ROOT);
        if(!username.matches("[a-z0-9._-]{3,60}"))throw new IllegalArgumentException("Tên đăng nhập từ 3 đến 60 ký tự, dùng chữ, số, dấu chấm, gạch ngang");
        validatePassword(request.password());
        if(!Set.of("CUSTOMER","OWNER").contains(String.valueOf(request.role())))throw new IllegalArgumentException("Chỉ được đăng ký khách hàng hoặc chủ xe");
        String name=request.displayName()==null?"":request.displayName().trim();
        if(name.length()<2||name.length()>100||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Cần nhập họ tên hợp lệ");
        String phone=request.phone()==null?"":request.phone().replaceAll("[\\s()-]","");
        if(!phone.matches("\\+?[0-9]{7,15}"))throw new IllegalArgumentException("Cần nhập số điện thoại hợp lệ");
        String email=request.email()==null?"":request.email().trim();
        if(email.length()>254||(!email.isEmpty()&&!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))throw new IllegalArgumentException("Email không hợp lệ");
        if(accounts.findByUsernameIgnoreCase(username).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Tên đăng nhập đã được sử dụng");
        Account account=new Account(username,passwords.encode(request.password()),request.role());account.setDisplayName(name);account.setPhone(phone);account.setEmail(email.isEmpty()?null:email);
        try{return ProfileView.of(accounts.saveAndFlush(account));}catch(DataIntegrityViolationException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,"Tên đăng nhập đã được sử dụng");}
    }
    @Transactional public void changePassword(String subject,String current,String next){
        Account account=accounts.findById(Long.parseLong(subject)).filter(Account::isEnabled).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(current==null||!passwords.matches(current,account.getPasswordHash()))throw new IllegalArgumentException("Mật khẩu hiện tại chưa đúng");
        validatePassword(next);account.setPasswordHash(passwords.encode(next));accounts.save(account);
    }
}
