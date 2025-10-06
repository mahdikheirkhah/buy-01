package com.backend.user_service.repository;
import com.backend.user_service.dto.updateUserDTO;
import com.backend.user_service.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring") // Tells MapStruct to create a Spring Bean
public interface UserMapper {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDto(updateUserDTO dto, @MappingTarget User entity);
}