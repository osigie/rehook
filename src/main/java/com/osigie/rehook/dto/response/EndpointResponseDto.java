package com.osigie.rehook.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EndpointResponseDto(UUID id, String url, boolean isActive, OffsetDateTime createdAt,
                                  OffsetDateTime updatedAt, EndpointAuthResponseDto endpointAuth) {
}
