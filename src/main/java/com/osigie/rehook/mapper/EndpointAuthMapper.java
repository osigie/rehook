package com.osigie.rehook.mapper;

import com.osigie.rehook.configuration.MapstructMapperConfig;
import com.osigie.rehook.domain.model.EndpointAuth;
import com.osigie.rehook.dto.request.EndpointAuthRequestDto;
import org.mapstruct.Mapper;

@Mapper(config = MapstructMapperConfig.class)
public interface EndpointAuthMapper {

    EndpointAuth mapEntity(EndpointAuthRequestDto source);

}
