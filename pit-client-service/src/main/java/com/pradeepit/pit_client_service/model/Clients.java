package com.pradeepit.pit_client_service.model;

import com.pradeepit.pit_client_service.model.base.Auditable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "clients")
public class Clients extends Auditable {

    @Id
    private String id;
    private String clientCode;
    private String company;
    private String email;
    private String status;
    private AddressDetails addressDetails;
    private ContactInfo contactInfo;
    private BusinessInfo businessInfo;
    private FileUpload fileUploads;
    private boolean deleted;
    private String customerId;
    @Transient
    private List<SpocDetail> spocDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressDetails {
        private String country;
        private String state;
        private String city;
        private String zipCode;
        private String street;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BusinessInfo {
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
    public static class ContactInfo {
        private String website;
        private String phone;
        private String telephone;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileUpload {
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