package com.osigie.rehook.controller;

import com.osigie.rehook.dto.request.EndpointRequestDto;
import com.osigie.rehook.dto.response.EndpointResponseDto;
import com.osigie.rehook.dto.response.SubscriptionResponseDto;
import com.osigie.rehook.model.Endpoint;
import com.osigie.rehook.model.Subscription;
import com.osigie.rehook.service.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public Page<Subscription> getSubscriptions(Pageable pageable) {
        return subscriptionService.findByTenantId("default", pageable);
    }

    @GetMapping("/{id}")
    public Subscription getSubscription(@PathVariable UUID id) {
        return subscriptionService.findById(id);
    }

    @PostMapping
    public Subscription createSubscription(@RequestBody Subscription subscription) {
        return subscriptionService.save(subscription);
    }

    @PostMapping("/{id}/endpoints")
    public ResponseEntity<SubscriptionResponseDto> createEndpoints(@RequestBody List<EndpointRequestDto> dto,  @PathVariable UUID id) {

//        TODO: use mapper
        List<Endpoint> endpoints = dto.stream().map((ep)-> Endpoint.builder()
                .isActive(ep.isActive())
                        .url(ep.url())
                .build()).toList();

       Subscription  endpointList = subscriptionService.addEndpoints(endpoints, id);

       //TODO use mapper
        List<EndpointResponseDto> endpointResponseDtos = endpointList.getEndpoints().stream().map((ep)-> new EndpointResponseDto(ep.getId(), ep.getUrl(), ep.isActive())).toList();

        SubscriptionResponseDto subscriptionResponseDto = new SubscriptionResponseDto(endpointResponseDtos, endpointList.getName(), endpointList.getId());

      return new ResponseEntity<>(subscriptionResponseDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/endpoints")
    public ResponseEntity<List<EndpointResponseDto>> getEndpoints(@PathVariable UUID id) {
        List<Endpoint> endpointList = subscriptionService.listEndpoints(id);

        List<EndpointResponseDto> endpointResponseDtos = endpointList.stream().map(e -> new EndpointResponseDto(e.getId(),e.getUrl(), e.isActive())).toList();

        return new ResponseEntity<>(endpointResponseDtos, HttpStatus.OK);

    }
}
