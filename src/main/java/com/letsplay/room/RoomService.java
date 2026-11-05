package com.letsplay.room;

import com.letsplay.auth.AuthService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoomService {
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final RoomRepository rooms;
    private final MembershipRepository memberships;
    private final AuthService auth;
    private final SecureRandom random = new SecureRandom();

    public RoomService(RoomRepository rooms, MembershipRepository memberships, AuthService auth) {
        this.rooms = rooms;
        this.memberships = memberships;
        this.auth = auth;
    }

    @Transactional
    public RoomResponse create(RoomRequests.CreateRoom request) {
        String requestId = request.requestId().toString();
        Room room = rooms.findByCreationRequestId(requestId).orElseGet(() -> {
            String code = newInviteCode();
            return rooms.save(new Room(UUID.randomUUID().toString(), request.roomName().trim(), code,
                    requestId, request.eventDate(), request.eventTime(), clean(request.region())));
        });
        return issueMembership(room, request.guestId(), request.displayName(), "HOST");
    }

    @Transactional
    public RoomResponse createForAccount(RoomRequests.CreateRoom request, String token) {
        String accountId=auth.accountId(token);
        String requestId=request.requestId().toString();
        Room room=rooms.findByCreationRequestId(requestId).orElseGet(() -> rooms.save(new Room(UUID.randomUUID().toString(),request.roomName().trim(),newInviteCode(),requestId,request.eventDate(),request.eventTime(),clean(request.region()))));
        return issueAccountMembership(room,accountId,request.displayName(),"HOST");
    }

    @Transactional
    public RoomResponse join(RoomRequests.JoinRoom request) {
        String inviteCode = request.inviteCode().trim().toUpperCase(Locale.ROOT);
        Room room = rooms.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã tham gia không hợp lệ."));
        if (!"OPEN".equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng hiện không nhận thành viên mới.");
        }
        return issueMembership(room, request.guestId(), request.displayName(), "MEMBER");
    }

    @Transactional
    public RoomResponse joinForAccount(RoomRequests.JoinRoom request, String token) {
        String accountId=auth.accountId(token);
        String inviteCode=request.inviteCode().trim().toUpperCase(Locale.ROOT);
        Room room=rooms.findByInviteCode(inviteCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Mã tham gia không hợp lệ."));
        if (!"OPEN".equals(room.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Phòng hiện không nhận thành viên mới.");
        return issueAccountMembership(room,accountId,request.displayName(),"MEMBER");
    }

    @Transactional(readOnly = true)
    public RoomPreview preview(RoomRequests.PreviewRoom request) {
        String inviteCode = request.inviteCode().trim().toUpperCase(Locale.ROOT);
        Room room = rooms.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã tham gia không hợp lệ."));
        if (!"OPEN".equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng hiện không nhận thành viên mới.");
        }
        return RoomPreview.from(room);
    }

    @Transactional(readOnly = true)
    public RoomResponse get(String roomId, String accessToken) {
        RoomAccess access = authorize(roomId, accessToken);
        return RoomResponse.from(access.room(), access.member(), null, memberships.countByRoomId(roomId));
    }

    @Transactional(readOnly = true)
    public RoomAccess authorize(String roomId, String accessToken) {
        Room room = rooms.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng."));
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên truy cập không hợp lệ.");
        }
        Membership member = memberships.findByRoomIdAndAccessTokenHash(roomId, sha256(accessToken)).orElse(null);
        if(member==null) {
            try { member=memberships.findByRoomIdAndAccountId(roomId,auth.accountId(accessToken)).orElse(null); }
            catch(ResponseStatusException ignored) { }
        }
        if(member==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Phiên đã hết hạn hoặc không hợp lệ.");
        return new RoomAccess(room, member);
    }

    @Transactional
    public void claim(String roomId,String accountToken,String guestToken) {
        String accountId=auth.accountId(accountToken);
        RoomAccess old=authorize(roomId,guestToken);
        if(old.member().getAccountId()!=null) return;
        if(memberships.findByRoomIdAndAccountId(roomId,accountId).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Tài khoản đã có quyền vào phòng này.");
        old.member().claim(accountId);
    }

    private RoomResponse issueAccountMembership(Room room,String accountId,String displayName,String role) {
        Membership member=memberships.findByRoomIdAndAccountId(room.getId(),accountId).orElse(null);
        if(member==null) { member=memberships.save(new Membership(UUID.randomUUID().toString(),room.getId(),accountId,displayName.trim(),role,true)); if("MEMBER".equals(role)) room.bumpRevision(); }
        return RoomResponse.from(room,member,null,memberships.countByRoomId(room.getId()));
    }

    private RoomResponse issueMembership(Room room, UUID guestId, String displayName, String role) {
        String token = randomToken();
        String guestIdHash = sha256(guestId.toString());
        Membership member = memberships.findByRoomIdAndGuestIdHash(room.getId(), guestIdHash).orElse(null);
        if (member == null) {
            member = memberships.save(new Membership(UUID.randomUUID().toString(), room.getId(),
                    guestIdHash, sha256(token), displayName.trim(), role));
            if ("MEMBER".equals(role)) room.bumpRevision();
        } else {
            member.refreshSession(sha256(token), displayName.trim());
        }
        return RoomResponse.from(room, member, token, memberships.countByRoomId(room.getId()));
    }

    private String newInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(6);
            for (int i = 0; i < 6; i++) code.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
            String candidate = code.toString();
            if (!rooms.existsByInviteCode(candidate)) return candidate;
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể tạo mã tham gia lúc này.");
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
