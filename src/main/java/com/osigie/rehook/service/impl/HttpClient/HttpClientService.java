package com.osigie.rehook.service.impl.HttpClient;

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
    private final AuthStrategyFactory authStrategyFactory;

    public HttpClientService(WebClient.Builder webClient, AuthStrategyFactory authStrategyFactory) {
        this.webClient = webClient.clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(10))
        )).build();
        this.authStrategyFactory = authStrategyFactory;
    }

    public HttpResponse send(Delivery delivery) {
        String url = delivery.getEndpoint().getUrl();
        String payload = delivery.getEvent().getPayload();

        Endpoint endpoint = delivery.getEndpoint();
        EndpointAuth endpointAuth = endpoint.getEndpointAuth();

        AuthStrategy authStrategy = authStrategyFactory.getAuthStrategy(endpointAuth.getAuthType());
        Map<String, String> securityHeaders = authStrategy.getHeaders(endpointAuth, payload);

        try {
            WebClient.RequestBodySpec request = this.webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            securityHeaders.forEach(request::header);

            ClientResponse response = request
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
