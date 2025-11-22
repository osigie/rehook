package com.osigie.rehook.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequestDto(@NotBlank String name) {
}
