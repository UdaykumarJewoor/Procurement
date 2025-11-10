package com.pradeepit.pit_client_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpocDetailsDTO {
    private String id;
    private String clientId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String department;
    private Boolean isActive;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")  // <-- CHANGED pattern
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")  // <-- CHANGED pattern
    private LocalDateTime updatedAt;
    private String createdBy;
    private String lastModifiedBy;
}