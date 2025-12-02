package com.osigie.rehook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterDto(@Email String email, @NotBlank String password, @NotBlank String tenant) {
}
