package com.osigie.rehook.service.impl.HttpClient;

import com.osigie.rehook.domain.model.EndpointAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class BasicAuthStrategy implements AuthStrategy {
    @Override
    public Map<String, String> getHeaders(EndpointAuth endpointAuth, String payload) {
        String token = Base64.getEncoder()
                .encodeToString((endpointAuth.getBasicUsername() + ":" + endpointAuth.getBasicPassword()).getBytes());

        return Map.of("Authorization", "Basic " + token);
    }
}
