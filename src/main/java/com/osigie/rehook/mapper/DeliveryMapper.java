package com.osigie.rehook.mapper;

import com.osigie.rehook.configuration.MapstructMapperConfig;
import com.osigie.rehook.domain.model.Delivery;
import com.osigie.rehook.dto.response.DeliveryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapstructMapperConfig.class, uses = {EndpointMapper.class, EventMapper.class, DeliveryAttemptMapper.class})
public interface DeliveryMapper {

    @Mapping(target = "endpoint", source = "endpoint")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "deliveryAttempts", source = "deliveryAttempts")
    DeliveryResponseDto mapDto(Delivery source);
}
