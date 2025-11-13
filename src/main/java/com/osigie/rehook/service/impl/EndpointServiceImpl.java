package com.osigie.rehook.service.impl;

import com.osigie.rehook.model.Endpoint;
import com.osigie.rehook.repository.EndpointRepository;
import com.osigie.rehook.service.EndpointService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EndpointServiceImpl implements EndpointService {
    private final EndpointRepository endpointRepository;

    public EndpointServiceImpl(EndpointRepository endpointRepository) {
        this.endpointRepository = endpointRepository;
    }

    @Override
    public Endpoint findById(UUID id) {
        return null;
    }

    @Override
    public Endpoint save(Endpoint endpoint) {
        var ep = Endpoint.builder().url("http://localhost:8080/endpoints").isActive(endpoint.isActive()).secret("secret").build();

        return endpointRepository.save(ep);
    }
}
