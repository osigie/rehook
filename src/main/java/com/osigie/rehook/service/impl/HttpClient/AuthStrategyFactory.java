package com.osigie.rehook.service.impl.HttpClient;

import com.osigie.rehook.domain.model.AuthType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthStrategyFactory {
    private final Map<AuthType, AuthStrategy> authStrategyMap;

    public AuthStrategyFactory(
            NoAuthStrategy noAuthStrategy,
            ApiKeyAuthStrategy apiKeyAuthStrategy,
            HmacAuthStrategy hmacAuthStrategy,
            BasicAuthStrategy basicAuthStrategy
    ) {
        this.authStrategyMap = Map.of(
                AuthType.NONE, noAuthStrategy,
                AuthType.BASIC_AUTH, basicAuthStrategy,
                AuthType.API_KEY, apiKeyAuthStrategy,
                AuthType.HmacSHA256, hmacAuthStrategy
        );
    }

    public AuthStrategy getAuthStrategy(AuthType authType) {
        return this.authStrategyMap.get(authType);
    }

}
