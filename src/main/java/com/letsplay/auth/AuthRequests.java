package com.letsplay.auth;
import jakarta.validation.constraints.*;
public final class AuthRequests {
    private AuthRequests() {}
    public record Register(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=8,max=72) String password,
                           @NotBlank @Size(max=60) String displayName) {}
    public record Login(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=72) String password) {}
}
