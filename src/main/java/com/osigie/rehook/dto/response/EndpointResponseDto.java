package com.osigie.rehook.dto.response;

import java.util.UUID;

public record EndpointResponseDto(UUID id, String url, boolean isActive) {
}
