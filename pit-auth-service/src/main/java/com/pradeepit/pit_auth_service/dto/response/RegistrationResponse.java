package com.pradeepit.pit_auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationResponse {

    private String id;
    private String customerNo;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String status;
    private String roleName;

}
