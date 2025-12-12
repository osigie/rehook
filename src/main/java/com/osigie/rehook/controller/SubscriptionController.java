package com.osigie.rehook.controller;

import com.osigie.rehook.domain.model.Endpoint;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.dto.request.EndpointRequestDto;
import com.osigie.rehook.dto.request.SubscriptionRequestDto;
import com.osigie.rehook.dto.response.DeliveryResponseDto;
import com.osigie.rehook.dto.response.EndpointResponseDto;
import com.osigie.rehook.dto.response.PageResponseDto;
import com.osigie.rehook.dto.response.SubscriptionResponseDto;
import com.osigie.rehook.mapper.EndpointMapper;
import com.osigie.rehook.mapper.SubscriptionMapper;
import com.osigie.rehook.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionMapper subscriptionMapper;
    private final EndpointMapper endpointMapper;

    public SubscriptionController(SubscriptionService subscriptionService, SubscriptionMapper subscriptionMapper, EndpointMapper endpointMapper) {
        this.subscriptionService = subscriptionService;
        this.subscriptionMapper = subscriptionMapper;
        this.endpointMapper = endpointMapper;
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<SubscriptionResponseDto>> getSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) OffsetDateTime toDate
    ) {
        Pageable pageable = PageRequest.of(page, size);
       Page<Subscription>  subscriptions = subscriptionService.list(fromDate, toDate, pageable);

        List<SubscriptionResponseDto> subResponse = subscriptions.getContent().stream()
                .map(subscriptionMapper::mapDto).collect(Collectors.toList());

        PageResponseDto<SubscriptionResponseDto> response = new PageResponseDto(
                subscriptions.getTotalElements(),
                subscriptions.getNumber(),
                subscriptions.getSize(),
                subResponse);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDto> getSubscription(@Valid @PathVariable UUID id) {
        Subscription subscription = subscriptionService.findById(id);
        return new ResponseEntity<>(subscriptionMapper.mapDto(subscription), HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponseDto> createSubscription(@Valid @RequestBody SubscriptionRequestDto dto) {
        Subscription subscription = subscriptionService.save(subscriptionMapper.mapEntity(dto));
        return new ResponseEntity<>(subscriptionMapper.mapDto(subscription), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/endpoints")
    public ResponseEntity<SubscriptionResponseDto> createEndpoints(@Valid @RequestBody List<EndpointRequestDto> dto, @PathVariable UUID id) {

        List<Endpoint> endpoints = endpointMapper.mapEntityList(dto);
        Subscription endpointList = subscriptionService.addEndpoints(endpoints, id);
        SubscriptionResponseDto subscriptionResponseDto = subscriptionMapper.mapDto(endpointList);

        return new ResponseEntity<>(subscriptionResponseDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/endpoints")
    public ResponseEntity<List<EndpointResponseDto>> getEndpoints(@Valid @PathVariable UUID id) {
        List<Endpoint> endpointList = subscriptionService.listEndpoints(id);
        System.out.println(endpointList.size());
        return new ResponseEntity<>(endpointMapper.mapDtoList(endpointList), HttpStatus.OK);

    }
}
