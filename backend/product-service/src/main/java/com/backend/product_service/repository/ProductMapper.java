package com.backend.product_service.repository;

import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring") // Tells MapStruct to create a Spring Bean
public interface ProductMapper {

    /**
     * This method updates an existing Product entity from a DTO.
     * @param dto The source DTO with potentially null fields.
     * @param entity The destination entity to be updated.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProductFromDto(UpdateProductDTO dto, @MappingTarget Product entity);
}