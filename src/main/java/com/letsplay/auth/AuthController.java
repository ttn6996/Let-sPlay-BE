package com.letsplay.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name="Accounts · Tài khoản", description="Đăng ký, đăng nhập và khôi phục danh sách phòng trên thiết bị khác.")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service){this.service=service;}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Operation(summary="Đăng ký tài khoản")
    public AuthService.AuthResponse register(@Valid @RequestBody AuthRequests.Register request){return service.register(request);}
    @PostMapping("/login") @Operation(summary="Đăng nhập")
    public AuthService.AuthResponse login(@Valid @RequestBody AuthRequests.Login request){return service.login(request);}
    @GetMapping("/me") @Operation(summary="Thông tin tài khoản hiện tại")
    public AuthService.Profile me(@RequestHeader(value="Authorization",required=false) String authorization){return service.me(bearer(authorization));}
    @GetMapping("/rooms") @Operation(summary="Danh sách phòng đã lưu")
    public List<AuthService.SavedRoom> rooms(@RequestHeader(value="Authorization",required=false) String authorization){return service.myRooms(bearer(authorization));}
    @PostMapping("/logout") @Operation(summary="Đăng xuất thiết bị hiện tại")
    public void logout(@RequestHeader(value="Authorization",required=false) String authorization){service.logout(bearer(authorization));}
    private static String bearer(String value){return value!=null && value.startsWith("Bearer ")?value.substring(7):null;}
}
