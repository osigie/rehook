package com.osigie.rehook.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DeliveryAttemptResponseDto(UUID id, int statusCode, OffsetDateTime executedAt,
                                         Map<String, Object> responseBody, Map<String, Object> responseHeaders,
                                         int duration) {
}
