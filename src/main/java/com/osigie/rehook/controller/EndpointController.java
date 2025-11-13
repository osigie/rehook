package com.osigie.rehook.controller;

import com.osigie.rehook.model.Endpoint;
import com.osigie.rehook.repository.EndpointRepository;
import com.osigie.rehook.service.EndpointService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/endpoints")
public class EndpointController {
private final EndpointService endpointService;
    public EndpointController(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    @PostMapping
    public Endpoint save(@RequestBody Endpoint endpoint) {
       return endpointService.save(endpoint);
    }
}
