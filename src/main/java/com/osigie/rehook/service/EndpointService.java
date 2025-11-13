package com.osigie.rehook.service;

import com.osigie.rehook.model.Endpoint;

import java.util.UUID;

public interface EndpointService {

    Endpoint findById(UUID id);
    Endpoint save(Endpoint endpoint);

}
