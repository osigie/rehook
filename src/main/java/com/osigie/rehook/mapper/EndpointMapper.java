package com.osigie.rehook.mapper;

import com.osigie.rehook.configuration.MapstructMapperConfig;
import com.osigie.rehook.domain.model.Endpoint;
import com.osigie.rehook.dto.request.EndpointRequestDto;
import com.osigie.rehook.dto.response.EndpointResponseDto;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;


@Mapper(config = MapstructMapperConfig.class, uses = {EndpointAuthMapper.class})
public interface EndpointMapper {

    Endpoint mapEntity(EndpointRequestDto source);

    @Mapping(target = "isActive", source = "active")
    EndpointResponseDto mapDto(Endpoint source);

    List<Endpoint> mapEntityList(List<EndpointRequestDto> source);

    List<EndpointResponseDto> mapDtoList(List<Endpoint> source);


}
