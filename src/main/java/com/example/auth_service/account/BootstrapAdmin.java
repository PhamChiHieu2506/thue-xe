package com.example.auth_service.account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(name="app.bootstrap-admin.enabled",havingValue="true",matchIfMissing=true)
public class BootstrapAdmin implements CommandLineRunner {
    private final AccountRepository accounts;private final PasswordEncoder encoder;private final String username,password;
    public BootstrapAdmin(AccountRepository accounts,PasswordEncoder encoder,@Value("${app.bootstrap-admin.username:admin}") String username,@Value("${app.bootstrap-admin.password:}") String password){this.accounts=accounts;this.encoder=encoder;this.username=username;this.password=password;}
    @Override public void run(String... args){
        if(accounts.existsByRole("ADMIN"))return;
        AccountService.validatePassword(password);
        if(accounts.findByUsernameIgnoreCase(username).isPresent())throw new IllegalStateException("Tên quản trị ban đầu đã có vai trò khác");
        accounts.saveAndFlush(new Account(username,encoder.encode(password),"ADMIN"));
    }
}
