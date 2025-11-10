package com.pradeepit.pit_auth_service.model;

import com.pradeepit.pit_auth_service.model.base.Auditable;
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
@Document(collection = "roles")
public class Role extends Auditable {

    @Id
    private String id;
    private String name;
    private String roleType;
    private String createdBy;
    private String lastModifiedBy;

}