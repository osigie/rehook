package com.osigie.rehook.mapper;

import com.osigie.rehook.configuration.MapstructMapperConfig;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.dto.request.SubscriptionRequestDto;
import com.osigie.rehook.dto.response.SubscriptionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(config = MapstructMapperConfig.class, uses = EndpointMapper.class)
public interface SubscriptionMapper {

    Subscription mapEntity(SubscriptionRequestDto source);

    @Mapping(target = "endpoints", source = "endpoints")
    SubscriptionResponseDto mapDto(Subscription source);
}
