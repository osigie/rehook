package com.osigie.rehook.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DeliveryAttemptResponseDto(UUID id, int statusCode, OffsetDateTime executedAt,
                                         Map<String, String> responseBody, Map<String, String> responseHeaders,
                                         int duration) {
}
