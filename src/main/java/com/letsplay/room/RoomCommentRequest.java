package com.letsplay.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCommentRequest(@NotBlank @Size(max = 500) String content) {}
