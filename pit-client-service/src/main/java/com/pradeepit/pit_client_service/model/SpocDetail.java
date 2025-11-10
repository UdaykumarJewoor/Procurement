package com.pradeepit.pit_client_service.model;

import com.pradeepit.pit_client_service.model.base.Auditable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "spoc_details")
public class SpocDetail extends Auditable {
    @Id
    private String id;
    private String clientId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String department;
    private Boolean isActive;
    private String createdBy;
    private String lastModifiedBy;
}