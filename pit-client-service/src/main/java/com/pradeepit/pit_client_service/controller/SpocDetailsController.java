package com.pradeepit.pit_client_service.controller;

import com.pradeepit.pit_client_service.dto.SpocDetailsDTO;
import com.pradeepit.pit_client_service.service.ClientService;
import com.pradeepit.pit_client_service.service.SpocDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Slf4j
@RequestMapping("/api/spocs")
@RestController
public class SpocDetailsController {

    @Autowired
    private SpocDetailsService spocDetailsService;
@Autowired
private ClientService clientService;

    @GetMapping("/read/spocId/{id}")
    public boolean isSpocPresent(@PathVariable String id) {
        return spocDetailsService.isSpocPresent(id);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createSpocDetails(@RequestBody SpocDetailsDTO spocDetailsDTO) {
        try {
            String customerId = clientService.getCustomerId();
            SpocDetailsDTO addedSpocDetails = spocDetailsService.createSpocDetails(customerId, spocDetailsDTO);
            log.info("Added SpocDetails with name {}", addedSpocDetails.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "SpocDetails added successfully."));
        } catch (Exception e) {
            log.error("Error while creating SpocDetails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/read")
    public ResponseEntity<?> getAllSpocDetails() {
        List<SpocDetailsDTO> spocDetailsDTOS = spocDetailsService.getAllSPocDetails();
        if (spocDetailsDTOS.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No SpocDetails found."));
        }
        return ResponseEntity.ok(spocDetailsDTOS);
    }

    @GetMapping("/read/spocId/{spocId}")
    public ResponseEntity<?> getSpocDetailsById(@PathVariable String spocId) {
        SpocDetailsDTO spocDetailsDTO = spocDetailsService.getSpocDetailsById(spocId);
        if (spocDetailsDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "SpocDetails not found."));
        }
        return ResponseEntity.ok(spocDetailsDTO);
    }

    @DeleteMapping("/delete/{spocId}")
    public ResponseEntity<?> deleteSpocDetails(@PathVariable String spocId) {
        boolean isDeleted = spocDetailsService.deleteSpocDetails(spocId);
        if (isDeleted) {
            return ResponseEntity.ok(Map.of("message", "SpocDetails deleted successfully."));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "SpocDetails not found."));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateSpocDetails(@PathVariable String id, @RequestBody SpocDetailsDTO spocDetailsDTO) {
        SpocDetailsDTO updatedSpocDetails = spocDetailsService.updateSpocDetails(id, spocDetailsDTO);
        if (updatedSpocDetails == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "SpocDetail with id " + id + " not found."));
        }
        return ResponseEntity.ok(updatedSpocDetails);
    }

    @GetMapping("/read/{clientId}")
    public ResponseEntity<?> getSpocDetailsByClientId(@PathVariable String clientId) {
        List<SpocDetailsDTO> spocDetails = spocDetailsService.getAllSpocDetailsByClientId(clientId);
        if (spocDetails.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No SpocDetails found for client with id " + clientId));
        }
        return ResponseEntity.ok(spocDetails);
    }
}
