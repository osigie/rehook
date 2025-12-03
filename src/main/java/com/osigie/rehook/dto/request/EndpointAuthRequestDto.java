package com.osigie.rehook.dto.request;

import com.osigie.rehook.domain.model.AuthType;
import jakarta.validation.constraints.NotNull;

public record EndpointAuthRequestDto(
        @NotNull
        AuthType authType,
                                     String apiKeyName,
                                     String apiKeyValue,
                                     String hmacSecret,
                                     String basicUsername,
                                     String basicPassword) {
}
