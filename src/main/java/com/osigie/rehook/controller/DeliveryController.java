package com.osigie.rehook.controller;

import com.osigie.rehook.domain.model.Delivery;
import com.osigie.rehook.dto.response.DeliveryResponseDto;
import com.osigie.rehook.dto.response.PageResponseDto;
import com.osigie.rehook.mapper.DeliveryMapper;
import com.osigie.rehook.service.DispatcherService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

//    TODO: enhance the pagination
    @GetMapping
    public ResponseEntity<PageResponseDto<DeliveryResponseDto>> listDeliveries() {
        PageRequest page = PageRequest.of(0, 10);
        Page<Delivery> deliveries = dispatcherService.listDeliveries(page);

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
