package com.letsplay.media;

import com.letsplay.auth.Account;
import com.letsplay.auth.AccountRepository;
import com.letsplay.auth.AccountSession;
import com.letsplay.auth.AccountSessionRepository;
import com.letsplay.room.Membership;
import com.letsplay.room.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhotoAuthService {

    private final AccountRepository accounts;
    private final AccountSessionRepository sessions;
    private final MembershipRepository memberships;

    public PhotoAuthService(AccountRepository accounts, AccountSessionRepository sessions, MembershipRepository memberships) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.memberships = memberships;
    }

    @Transactional
    public UserIdentity resolveIdentity(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String tokenHash = sha256(token.trim());

        // 1. Check account sessions
        Optional<AccountSession> sessionOpt = sessions.findByTokenHash(tokenHash)
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()));
        if (sessionOpt.isPresent()) {
            Account account = accounts.findById(sessionOpt.get().getAccountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại."));
            return new UserIdentity(account.getId(), account.getDisplayName(), "USER");
        }

        // 2. Check room membership tokens
        Optional<Membership> memberOpt = memberships.findByAccessTokenHash(tokenHash);
        if (memberOpt.isPresent()) {
            Membership member = memberOpt.get();
            if (member.getAccountId() != null) {
                Account account = accounts.findById(member.getAccountId()).orElse(null);
                if (account != null) {
                    return new UserIdentity(account.getId(), account.getDisplayName(), member.getRole());
                }
            }
            // Auto-provision guest account
            String guestEmail = "guest-" + member.getId() + "@guest.letsplay";
            Account guestAccount = accounts.findByEmail(guestEmail).orElseGet(() ->
                    accounts.save(new Account(UUID.randomUUID().toString(), guestEmail, null, member.getDisplayName()))
            );
            member.claim(guestAccount.getId());
            memberships.save(member);
            return new UserIdentity(guestAccount.getId(), guestAccount.getDisplayName(), member.getRole());
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập hoặc token không hợp lệ.");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
