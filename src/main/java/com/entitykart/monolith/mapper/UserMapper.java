package com.entitykart.monolith.mapper;

import com.entitykart.monolith.dto.UserDTO;
import com.entitykart.monolith.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO toDTO(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setEmail(entity.getEmail());
        dto.setRole(entity.getRole());
        dto.setActive(entity.isActive());
        dto.setGender(entity.getGender());
        dto.setContactNum(entity.getContactNum());
        dto.setProfilePicURL(entity.getProfilePicURL());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UserEntity toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setRole(dto.getRole());
        entity.setActive(dto.getActive());
        entity.setGender(dto.getGender());
        entity.setContactNum(dto.getContactNum());
        entity.setProfilePicURL(dto.getProfilePicURL());
        return entity;
    }
}
