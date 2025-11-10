package com.pradeepit.pit_auth_service.model;


import com.pradeepit.pit_auth_service.model.base.Auditable;
import com.pradeepit.pit_auth_service.model.enums.ActionEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "permissions")
public class Permission extends Auditable {

    @Id
    private String id;
    @Indexed
    private String module;
    @Indexed
    private ActionEnum action;  // Example: "READ", "CREATE", "UPDATE", "DELETE"
    private Set<String> roles;
}
