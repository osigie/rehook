package com.osigie.rehook.controller;

import com.osigie.rehook.service.DispatcherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("deliveries")
public class DeliveryController {
    private final DispatcherService dispatcherService;

    public DeliveryController(DispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    @PostMapping
    public ResponseEntity<?> retry(@Valid @RequestBody List<UUID> deliveryIds) {
        dispatcherService.dispatchDeliveriesAsync(deliveryIds);
        return new ResponseEntity<>(HttpStatus.OK);

    }


}
