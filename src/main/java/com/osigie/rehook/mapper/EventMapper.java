package com.osigie.rehook.mapper;

import com.osigie.rehook.configuration.MapstructMapperConfig;
import com.osigie.rehook.domain.model.Event;
import com.osigie.rehook.dto.response.EventResponseDto;
import org.mapstruct.Mapper;

@Mapper(config = MapstructMapperConfig.class)
public interface EventMapper {

    EventResponseDto mapDto(Event source);
}

