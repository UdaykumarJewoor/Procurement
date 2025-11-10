package com.pradeepit.pit_client_service.service;

import com.pradeepit.pit_client_service.dto.SpocDetailsDTO;
import com.pradeepit.pit_client_service.mapper.SpocDetailsMapper;
import com.pradeepit.pit_client_service.model.SpocDetail;
import com.pradeepit.pit_client_service.repository.SpocDetailsRepository;
import com.pradeepit.pit_client_service.util.IdGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class SpocDetailsService {

    private static final Logger log = LoggerFactory.getLogger(SpocDetailsService.class);

    @Autowired
    private SpocDetailsRepository spocDetailsRepository;
    @Autowired
    private IdGenerationService idGenerationService;
    @Autowired
    private SpocDetailsMapper spocDetailsMapper;

    @Autowired
    private ClientService clientService;
    public boolean isSpocPresent(String id) {
        Optional<SpocDetail> spocOptional = spocDetailsRepository.findByIdIgnoreCase(id);
        if (spocOptional.isEmpty()) {
            log.info("Spoc not found with id: {}", id);
            return false;
        }
        return true;
    }

    public SpocDetailsDTO createSpocDetails(String customerId, SpocDetailsDTO spocDetailsDTO) {
        SpocDetail spocDetail = spocDetailsMapper.mapToEntity(spocDetailsDTO);
        String spocId = idGenerationService.generateSpocId();
        spocDetail.setId(spocId);
        spocDetail.setCreatedAt(LocalDateTime.now());
        spocDetail.setUpdatedAt(null);
        spocDetail.setCreatedBy(customerId);
        spocDetail.setLastModifiedBy(null);
        SpocDetail savedSpocDetail = spocDetailsRepository.save(spocDetail);
        return spocDetailsMapper.mapToDto(savedSpocDetail);
    }

    public SpocDetailsDTO getSpocDetailsById(String spocId) {
        SpocDetail spocDetail = spocDetailsRepository.findByIdIgnoreCase(spocId).orElse(null);
        return spocDetailsMapper.mapToDto(spocDetail);
    }

    public List<SpocDetailsDTO> getAllSPocDetails() {
        return spocDetailsRepository.findAll()
                .stream()
                .map(spocDetailsMapper::mapToDto)
                .collect(Collectors.toList());
    }

    public boolean deleteSpocDetails(String spocId) {
        SpocDetail spocDetail = spocDetailsRepository.findByIdIgnoreCase(spocId).orElse(null);
        if (spocDetail != null) {
            spocDetailsRepository.delete(spocDetail);
            return true;
        } else {
            return false;
        }
    }

    public SpocDetailsDTO updateSpocDetails(String spocId, SpocDetailsDTO spocDetailsDTO) {
        String customerId = clientService.getCustomerId();
        spocDetailsDTO.setLastModifiedBy(customerId);

        Optional<SpocDetail> spocDetailOptional = spocDetailsRepository.findByIdIgnoreCase(spocId);

        if (spocDetailOptional.isPresent()) {
            SpocDetail existingSpocDetail = spocDetailOptional.get();

            existingSpocDetail.setUpdatedAt(LocalDateTime.now());

            // Update only non-null fields
            if (spocDetailsDTO.getName() != null && !spocDetailsDTO.getName().equals(existingSpocDetail.getName())) {
                existingSpocDetail.setName(spocDetailsDTO.getName());
            }
            if (spocDetailsDTO.getPhone() != null && !spocDetailsDTO.getPhone().equals(existingSpocDetail.getPhone())) {
                existingSpocDetail.setPhone(spocDetailsDTO.getPhone());
            }
            if (spocDetailsDTO.getEmail() != null && !spocDetailsDTO.getEmail().equals(existingSpocDetail.getEmail())) {
                existingSpocDetail.setEmail(spocDetailsDTO.getEmail());
            }
            if (spocDetailsDTO.getRole() != null && !spocDetailsDTO.getRole().equals(existingSpocDetail.getRole())) {
                existingSpocDetail.setRole(spocDetailsDTO.getRole());
            }
            if (spocDetailsDTO.getDepartment() != null && !spocDetailsDTO.getDepartment().equals(existingSpocDetail.getDepartment())) {
                existingSpocDetail.setDepartment(spocDetailsDTO.getDepartment());
            }
            if (spocDetailsDTO.getIsActive() != null && !spocDetailsDTO.getIsActive().equals(existingSpocDetail.getIsActive())) {
                existingSpocDetail.setIsActive(!existingSpocDetail.getIsActive());
            }
            SpocDetail updatedSpocDetail = spocDetailsRepository.save(existingSpocDetail);
            return spocDetailsMapper.mapToDto(updatedSpocDetail);
        } else {
            log.error("SPOC not found with id {}", spocId);
            return null;
        }
    }

    public List<SpocDetailsDTO> getAllSpocDetailsByClientId(String clientId) {
        List<SpocDetail> spocDetails = spocDetailsRepository.findByClientIdIgnoreCase(clientId);
        return spocDetails.stream()
                .map(spocDetailsMapper::mapToDto)
                .collect(Collectors.toList());
    }


}

