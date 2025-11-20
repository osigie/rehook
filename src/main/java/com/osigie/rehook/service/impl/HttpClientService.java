package com.osigie.rehook.service.impl;

import com.osigie.rehook.domain.HttpResponse;
import com.osigie.rehook.domain.model.Delivery;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
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
