package com.osigie.rehook.repository;

import com.osigie.rehook.domain.model.Delivery;
import com.osigie.rehook.domain.model.DeliveryStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    @Query("""
            SELECT d.id FROM Delivery  d WHERE d.status = :status and d.nextRetryAt < :now
            """)
    List<UUID> findAllByStatusAndNextRetryAtBefore(@Param("status") DeliveryStatusEnum status, @Param("now") OffsetDateTime now);
}
