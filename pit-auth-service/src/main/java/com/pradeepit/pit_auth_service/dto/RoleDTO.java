package com.pradeepit.pit_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDTO {
    private String id;
    private String name;
    private String roleType;
    @JsonFormat(pattern = "dd EEEE yyyy HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonFormat(pattern = "dd EEEE yyyy HH:mm:ss")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
