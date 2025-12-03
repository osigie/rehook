package com.osigie.rehook.service.impl.HttpClient;

import com.osigie.rehook.domain.model.EndpointAuth;

import java.util.Map;

public interface AuthStrategy {
    Map<String, String> getHeaders(EndpointAuth endpointAuth, String payload);
}
