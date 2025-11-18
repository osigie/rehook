package com.osigie.rehook.repository;

import com.osigie.rehook.model.Delivery;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface DeliveryRepository extends CrudRepository<Delivery, UUID> {
}
