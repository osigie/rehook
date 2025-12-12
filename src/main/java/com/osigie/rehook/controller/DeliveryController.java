package com.osigie.rehook.controller;

import com.osigie.rehook.domain.model.Delivery;
import com.osigie.rehook.domain.model.DeliveryStatusEnum;
import com.osigie.rehook.dto.response.DeliveryResponseDto;
import com.osigie.rehook.dto.response.PageResponseDto;
import com.osigie.rehook.mapper.DeliveryMapper;
import com.osigie.rehook.service.DispatcherService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
    private final DispatcherService dispatcherService;
    private final DeliveryMapper deliveryMapper;

    public DeliveryController(DispatcherService dispatcherService, DeliveryMapper deliveryMapper) {
        this.dispatcherService = dispatcherService;
        this.deliveryMapper = deliveryMapper;
    }

    @PostMapping
    public ResponseEntity<?> retry(@Valid @RequestBody List<UUID> deliveryIds) {
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @GetMapping
    public ResponseEntity<PageResponseDto<DeliveryResponseDto>> listDeliveries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) OffsetDateTime toDate,
            @RequestParam(required = false) DeliveryStatusEnum status
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Delivery> deliveries = dispatcherService.listDeliveries(pageable, fromDate, toDate, status);

        List<DeliveryResponseDto> deliveryResponse = deliveries.getContent().stream()
                .map(deliveryMapper::mapDto).collect(Collectors.toList());

        PageResponseDto<DeliveryResponseDto> response = new PageResponseDto(
                deliveries.getTotalElements(),
                deliveries.getNumber(),
                deliveries.getSize(),
                deliveryResponse);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @GetMapping("{id}")
    public ResponseEntity<DeliveryResponseDto> getDelivery(@Valid @PathVariable UUID id) {
        Delivery delivery = dispatcherService.getDelivery(id);
        DeliveryResponseDto deliveryResponseDto = deliveryMapper.mapDto(delivery);
        return new ResponseEntity<>(deliveryResponseDto, HttpStatus.OK);
    }


}
