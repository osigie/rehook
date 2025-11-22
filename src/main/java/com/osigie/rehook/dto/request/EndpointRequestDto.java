package com.osigie.rehook.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record EndpointRequestDto(@NotNull boolean isActive,
                                 @NotBlank @URL @Size(max = 2048, message = "URL must not exceed 2048 characters") String url) {
}
