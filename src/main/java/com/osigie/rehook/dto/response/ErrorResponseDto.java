package com.osigie.rehook.dto.response;

import org.springframework.http.HttpStatus;

public record ErrorResponseDto(HttpStatus status, String message, int statusCode) {
}
