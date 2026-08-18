package com.entitykart.monolith.mapper;

import com.entitykart.monolith.dto.AddressDTO;
import com.entitykart.monolith.entity.AddressEntity;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressDTO toDTO(AddressEntity entity) {
        if (entity == null) {
            return null;
        }
        AddressDTO dto = new AddressDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setFullName(entity.getFullName());
        dto.setPhone(entity.getPhone());
        dto.setStreetAddress(entity.getStreetAddress());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setZipCode(entity.getZipCode());
        dto.setIsDefault(entity.getIsDefault());
        return dto;
    }

    public AddressEntity toEntity(AddressDTO dto) {
        if (dto == null) {
            return null;
        }
        AddressEntity entity = new AddressEntity();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setStreetAddress(dto.getStreetAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZipCode(dto.getZipCode());
        entity.setIsDefault(dto.getIsDefault());
        return entity;
    }
}
