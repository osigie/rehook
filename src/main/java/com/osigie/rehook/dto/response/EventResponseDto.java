package com.osigie.rehook.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;

public record EventResponseDto(String payload, Map<String, Object> headers, OffsetDateTime receivedAt) {
}
