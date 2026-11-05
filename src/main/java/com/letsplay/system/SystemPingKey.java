package com.letsplay.system;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class SystemPingKey {
    private final String value;

    public SystemPingKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String value() { return value; }

    public boolean matches(String candidate) {
        return candidate != null && MessageDigest.isEqual(
                value.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8));
    }
}
