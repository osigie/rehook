package com.osigie.rehook.dto.response;

import com.osigie.rehook.domain.model.AuthType;

import java.util.UUID;

public record EndpointAuthResponseDto(
        UUID id,
        AuthType authType,
        String apiKeyName,
        String apiKeyValue,
        String hmacSecret,
        String basicUsername,
        String basicPassword) {
}
