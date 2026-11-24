package com.example.auth_service.account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountRepository extends JpaRepository<Account,Long> {
    Optional<Account> findByUsername(String username);
    Optional<Account> findByUsernameIgnoreCase(String username);
    boolean existsByRole(String role);
}
