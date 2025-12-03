package com.osigie.rehook.service.impl;

import com.osigie.rehook.domain.HttpResponse;
import com.osigie.rehook.domain.model.AuthType;
import com.osigie.rehook.domain.model.Delivery;
import com.osigie.rehook.domain.model.Endpoint;
import com.osigie.rehook.domain.model.EndpointAuth;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class HttpClientService {

    private final WebClient webClient;

    public HttpClientService(WebClient.Builder webClient) {
        this.webClient = webClient.clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(10))
        )).build();
    }

    public Map<String, String> processEndpointSecurity(EndpointAuth endpointAuth) {
        Map<String, String> map = new HashMap<>();
        AuthType authType = endpointAuth.getAuthType();
        switch (authType) {
            case NONE:
                break;
            case API_KEY:
                map.put("auth", endpointAuth.getApiKeyName());
                map.put("auth_value", endpointAuth.getApiKeyValue());
                break;
            case BASIC_AUTH:
                String basic = Base64.getEncoder()
                        .encodeToString((endpointAuth.getBasicUsername() + ":" + endpointAuth.getBasicPassword()).getBytes());

                map.put("auth", "Authorization");
                map.put("auth_value", "Bearer " + basic);
                break;
        }
        return map;
    }

    public HttpResponse send(Delivery delivery) {
        String url = delivery.getEndpoint().getUrl();
        String payload = delivery.getEvent().getPayload();

        try {
            ClientResponse response = this.webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .exchange().block();

            int status = response.statusCode().value();

            Map<String, String> headers = response.headers().asHttpHeaders().toSingleValueMap();

            Map<String, String> body = response.bodyToMono(Map.class).blockOptional().orElse(Map.of());
            return new HttpResponse(status, headers, body);

        } catch (Exception e) {
            return new HttpResponse(500, Map.of(), Map.of("error", e.getMessage()));
        }
    }

}
