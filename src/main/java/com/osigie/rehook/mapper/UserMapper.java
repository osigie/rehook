package com.osigie.rehook.mapper;

import com.osigie.rehook.configuration.MapstructMapperConfig;
import com.osigie.rehook.domain.model.User;
import com.osigie.rehook.dto.response.UserResponseDto;
import org.mapstruct.Mapper;

@Mapper(config = MapstructMapperConfig.class)
public interface UserMapper {
    //TODO: add tenant
    UserResponseDto mapDto(User source);
}
