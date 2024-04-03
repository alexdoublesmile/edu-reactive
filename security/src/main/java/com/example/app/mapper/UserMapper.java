package com.example.app.mapper;

import com.example.app.model.dto.UserDto;
import com.example.app.model.entity.UserEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(UserEntity entity);
    @InheritInverseConfiguration
    UserEntity toEntity(UserDto dto);
}
