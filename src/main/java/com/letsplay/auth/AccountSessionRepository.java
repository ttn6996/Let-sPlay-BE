package com.letsplay.auth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountSessionRepository extends JpaRepository<AccountSession,String> {
    Optional<AccountSession> findByTokenHash(String tokenHash);
}
