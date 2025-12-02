package com.osigie.rehook.dto.response;


import java.util.UUID;

public record UserResponseDto(UUID id, String email, String tenant) {
}
