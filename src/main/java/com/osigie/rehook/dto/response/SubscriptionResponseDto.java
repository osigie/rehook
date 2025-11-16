package com.osigie.rehook.dto.response;

import java.util.List;
import java.util.UUID;

public record SubscriptionResponseDto(List<EndpointResponseDto> endpoints, String name, UUID id) {
}
