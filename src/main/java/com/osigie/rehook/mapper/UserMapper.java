package com.osigie.rehook.mapper;

import com.osigie.rehook.configuration.MapstructMapperConfig;
import com.osigie.rehook.domain.model.User;
import com.osigie.rehook.dto.response.UserResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapstructMapperConfig.class)
public interface UserMapper {

    @Mapping(target = "tenant", source = "tenant.name")
    UserResponseDto mapDto(User source);
}
