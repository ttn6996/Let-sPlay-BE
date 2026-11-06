package com.letsplay.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleIdentityVerifier {
    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId=clientId;
        this.verifier=new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId.isBlank()?"unconfigured":clientId))
                .build();
    }

    public GoogleIdentity verify(String credential) {
        if(clientId.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Đăng nhập Google chưa được cấu hình trên máy chủ.");
        try {
            GoogleIdToken token=verifier.verify(credential);
            if(token==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Thông tin đăng nhập Google không hợp lệ hoặc đã hết hạn.");
            GoogleIdToken.Payload payload=token.getPayload();
            return new GoogleIdentity(payload.getSubject(),payload.getEmail(),Boolean.TRUE.equals(payload.getEmailVerified()),
                    (String)payload.get("name"),(String)payload.get("hd"));
        } catch (GeneralSecurityException|IOException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Không thể xác minh tài khoản Google.");
        }
    }

    public record GoogleIdentity(String subject,String email,boolean emailVerified,String name,String hostedDomain) {}
}
