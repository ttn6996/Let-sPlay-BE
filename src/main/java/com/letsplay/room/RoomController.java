package com.letsplay.room;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService service;

    public RoomController(RoomService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody RoomRequests.CreateRoom request) {
        return service.create(request);
    }

    @PostMapping("/join")
    public RoomResponse join(@Valid @RequestBody RoomRequests.JoinRoom request) {
        return service.join(request);
    }

    @PostMapping("/preview")
    public RoomPreview preview(@Valid @RequestBody RoomRequests.PreviewRoom request) {
        return service.preview(request);
    }

    @GetMapping("/{roomId}")
    public RoomResponse get(@PathVariable String roomId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : null;
        return service.get(roomId, token);
    }
}
