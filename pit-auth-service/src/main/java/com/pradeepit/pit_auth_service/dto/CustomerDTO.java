package com.pradeepit.pit_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDTO {
    private String id;
    private String customerNo;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String status;
    private String customerStatus;
    private String image;
    private String roleName;
    @JsonIgnore
    private RoleDTO role;
    @JsonFormat(pattern = "dd MMMM yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "dd MMMM yyyy HH:mm:ss")
    private LocalDateTime updatedAt;
}