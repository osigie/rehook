package com.osigie.rehook.domain;

import java.util.Map;

public record HttpResponse(int code, Map<String, String> headers, Map<String, String> body) {
}
