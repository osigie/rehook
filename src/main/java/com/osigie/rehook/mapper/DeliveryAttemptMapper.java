package com.osigie.rehook.mapper;

import com.osigie.rehook.configuration.MapstructMapperConfig;
import com.osigie.rehook.domain.model.DeliveryAttempt;
import com.osigie.rehook.dto.response.DeliveryAttemptResponseDto;
import org.mapstruct.Mapper;

@Mapper(config = MapstructMapperConfig.class)
public interface DeliveryAttemptMapper {

    DeliveryAttemptResponseDto mapDto(DeliveryAttempt source);
}
