package com.osigie.rehook.dto.request;

import com.osigie.rehook.domain.model.AuthType;

public record EndpointAuthRequestDto(AuthType authType,
                                     String apiKeyName,
                                     String apiKeyValue,
                                     String hmacSecret,
                                     String basicUsername,
                                     String basicSecret) {
}
