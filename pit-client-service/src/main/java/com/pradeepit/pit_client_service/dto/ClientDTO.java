package com.pradeepit.pit_client_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientDTO {

    @Id
    private String id;
    private String clientCode;
    private String company;
    private String email;
    private String status;
    private AddressDetailsDTO addressDetails;
    private ContactInfoDTO contactInfo;
    private BusinessInfoDTO businessInfo;
    private FileUploadDTO fileUploads;
    private boolean deleted;
    private String customerId;
    @Transient
    private List<SpocDetailsDTO> spocDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressDetailsDTO {
        private String country;
        private String state;
        private String city;
        private String zipCode;
        private String street;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BusinessInfoDTO {
        private LocalDate registrationDate;
        private String industry;
        private String gstNo;
        private String tanNo;
        private String currency;
        private String status;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContactInfoDTO {
        private String website;
        private String phone;
        private String telephone;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileUploadDTO {
        private String companyLogo;
        private String gstFile;
        private String tanFile;
    }

    private String createdBy;
    private String lastModifiedBy;

    public void generateClientCode() {
        String firstName = company.split("\\s+")[0];
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String year = String.valueOf(LocalDate.now().getYear());
        this.clientCode = firstName.toUpperCase() + "-" + month + "-" + year;
    }
}
