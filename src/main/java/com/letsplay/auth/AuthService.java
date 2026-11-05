package com.letsplay.auth;

import com.letsplay.room.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AccountRepository accounts;
    private final AccountSessionRepository sessions;
    private final MembershipRepository memberships;
    private final RoomRepository rooms;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    public AuthService(AccountRepository accounts, AccountSessionRepository sessions, MembershipRepository memberships, RoomRepository rooms) {
        this.accounts=accounts; this.sessions=sessions; this.memberships=memberships; this.rooms=rooms;
    }

    @Transactional
    public AuthResponse register(AuthRequests.Register request) {
        String email=normalize(request.email());
        if (accounts.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email này đã được đăng ký.");
        Account account=accounts.save(new Account(UUID.randomUUID().toString(), email, passwords.encode(request.password()), request.displayName().trim()));
        return issue(account);
    }
    @Transactional
    public AuthResponse login(AuthRequests.Login request) {
        Account account=accounts.findByEmail(normalize(request.email())).orElse(null);
        if (account==null || !passwords.matches(request.password(), accountPassword(account))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email hoặc mật khẩu không chính xác.");
        return issue(account);
    }
    // Password verification uses a dedicated query projection so the hash never leaves the service.
    private String accountPassword(Account account) { return account.getPasswordHash(); }
    @Transactional(readOnly=true)
    public Profile me(String token) { Account a=account(token); return new Profile(a.getId(),a.getEmail(),a.getDisplayName()); }
    @Transactional(readOnly=true)
    public String accountId(String token) { return account(token).getId(); }
    @Transactional(readOnly=true)
    public List<SavedRoom> myRooms(String token) {
        Account account=account(token);
        return memberships.findAllByAccountIdOrderByCreatedAtDesc(account.getId()).stream().map(m -> rooms.findById(m.getRoomId()).map(r -> new SavedRoom(r.getId(),r.getName(),r.getInviteCode(),r.getPhase(),r.getStatus(),r.getEventDate(),r.getEventTime(),r.getRegion(),r.getCreatedAt(),r.getRevision(),memberships.countByRoomId(r.getId()),m.getRole())).orElse(null)).filter(Objects::nonNull).toList();
    }
    @Transactional
    public void logout(String token) { if(token!=null) sessions.findByTokenHash(sha256(token)).ifPresent(s -> sessions.deleteById(s.getId())); }
    private Account account(String token) {
        if(token==null || token.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Vui lòng đăng nhập.");
        AccountSession session=sessions.findByTokenHash(sha256(token)).filter(s -> s.getExpiresAt().isAfter(Instant.now())).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Phiên đăng nhập đã hết hạn."));
        return accounts.findById(session.getAccountId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Tài khoản không tồn tại."));
    }
    private AuthResponse issue(Account account) {
        byte[] bytes=new byte[32]; random.nextBytes(bytes); String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new AccountSession(UUID.randomUUID().toString(),account.getId(),sha256(token),Instant.now().plus(Duration.ofDays(30))));
        return new AuthResponse(token,Instant.now().plus(Duration.ofDays(30)),new Profile(account.getId(),account.getEmail(),account.getDisplayName()));
    }
    private static String normalize(String email){return email.trim().toLowerCase(Locale.ROOT);}
    private static String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public record Profile(String id,String email,String displayName) {}
    public record AuthResponse(String accessToken,Instant expiresAt,Profile account) {}
    public record SavedRoom(String id,String name,String inviteCode,String phase,String status,LocalDate eventDate,LocalTime eventTime,String region,Instant createdAt,long revision,long memberCount,String role) {}
}
