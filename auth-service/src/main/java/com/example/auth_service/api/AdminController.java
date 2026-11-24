package com.example.auth_service.api;
import com.example.auth_service.account.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/api/admin/accounts")
public class AdminController {
    private final AccountRepository accounts;
    public AdminController(AccountRepository accounts){this.accounts=accounts;}
    private void admin(Jwt jwt){var a=accounts.findById(Long.valueOf(jwt.getSubject())).orElseThrow();if(!a.isEnabled()||!"ADMIN".equals(a.getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
    public record View(Long id,String username,String role,boolean enabled,String displayNameMasked,String phoneMasked,String emailMasked){}
    private View view(Account a){return new View(a.getId(),a.getUsername(),a.getRole(),a.isEnabled(),Privacy.name(a.getDisplayName()),Privacy.phone(a.getPhone()),Privacy.email(a.getEmail()));}
    @GetMapping public List<View> list(@AuthenticationPrincipal Jwt jwt){admin(jwt);return accounts.findAll().stream().map(this::view).toList();}
    public record Change(boolean enabled){}
    @PutMapping("/{id}/enabled") public View enabled(@PathVariable Long id,@RequestBody Change change,@AuthenticationPrincipal Jwt jwt){admin(jwt);var a=accounts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if("ADMIN".equals(a.getRole()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không khóa tài khoản quản trị bằng thao tác này");a.setEnabled(change.enabled());return view(accounts.saveAndFlush(a));}
}
