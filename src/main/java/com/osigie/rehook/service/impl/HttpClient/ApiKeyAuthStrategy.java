package com.osigie.rehook.service.impl.HttpClient;

import com.osigie.rehook.domain.model.EndpointAuth;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ApiKeyAuthStrategy implements AuthStrategy {
    @Override
    public Map<String, String> getHeaders(EndpointAuth endpointAuth, String payload) {
        return Map.of(endpointAuth.getApiKeyName(), endpointAuth.getApiKeyValue());
    }
}
