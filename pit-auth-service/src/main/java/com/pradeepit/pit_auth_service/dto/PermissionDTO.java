package com.pradeepit.pit_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pradeepit.pit_auth_service.model.enums.ActionEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDTO {

    private String id;
    private String module;
    private ActionEnum action;  // Example: "READ", "WRITE", "DELETE"
    private Set<String> roles;
    @JsonFormat(pattern = "dd EEEE yyyy HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonFormat(pattern = "dd EEEE yyyy HH:mm:ss")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
