package com.pradeepit.pit_auth_service.model;


import com.pradeepit.pit_auth_service.model.base.Auditable;
import com.pradeepit.pit_auth_service.model.enums.CustomerStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "customers")
public class Customer extends Auditable {

    @Id
    private String id;
    private String customerNo;
    @Indexed(unique = true)
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private String status;
    private CustomerStatus customerStatus;
    private String accessToken;
    private String image;
    @DBRef
    private Role role;
    private String roleName;
    private boolean isFrozen;
    private int loginAttempts;  // Track login attempts
    private int otpAttempts;               // Number of OTP attempts
    private LocalDateTime frozenUntil;          // When the account will be unfrozen after too many failed OTP attempts
    private String otp;                         // The OTP generated
    private LocalDateTime otpGeneratedAt;       // When the OTP was generated
    private LocalDateTime otpExpiredAt;         // When the OTP expires

}