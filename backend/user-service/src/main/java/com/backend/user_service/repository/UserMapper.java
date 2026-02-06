package com.backend.user_service.repository;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.model.User;

@Mapper(componentModel = "spring") // Tells MapStruct to create a Spring Bean
public interface UserMapper {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDto(UpdateUserDTO dto, @MappingTarget User entity);
}