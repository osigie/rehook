package com.osigie.rehook.repository;


import com.osigie.rehook.model.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {
}
