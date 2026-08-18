package com.entitykart.monolith.service;

import com.entitykart.monolith.dto.AddressDTO;
import com.entitykart.monolith.entity.AddressEntity;
import com.entitykart.monolith.mapper.AddressMapper;
import com.entitykart.monolith.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    @Transactional
    public AddressDTO addAddress(Long userId, AddressDTO dto) {
        AddressEntity entity = new AddressEntity();
        entity.setUserId(userId);
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setStreetAddress(dto.getStreetAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZipCode(dto.getZipCode());
        entity.setIsDefault(dto.getIsDefault());

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            addressRepository.findByUserId(userId).forEach(addr -> {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            });
        }
        AddressEntity saved = addressRepository.save(entity);
        return addressMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressDTO> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId)
                .stream().map(addressMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        addressRepository.deleteByUserIdAndId(userId, addressId);
    }

    @Transactional
    public AddressDTO updateAddress(Long userId, Long addressId, AddressDTO dto) {
        AddressEntity entity = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setStreetAddress(dto.getStreetAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZipCode(dto.getZipCode());
        if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(entity.getIsDefault())) {
            addressRepository.findByUserId(userId).forEach(addr -> addr.setIsDefault(false));
            entity.setIsDefault(true);
        } else {
            entity.setIsDefault(dto.getIsDefault());
        }
        return addressMapper.toDTO(addressRepository.save(entity));
    }
}
