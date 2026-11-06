package com.letsplay.auth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountRepository extends JpaRepository<Account,String> {
    Optional<Account> findByEmail(String email);
    Optional<Account> findByGoogleSubject(String googleSubject);
    boolean existsByEmail(String email);
}
