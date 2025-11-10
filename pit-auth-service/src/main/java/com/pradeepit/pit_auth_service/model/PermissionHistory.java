package com.pradeepit.pit_auth_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "permission_history")
public class PermissionHistory {
    @Id
    private String id;
    private String permissionId;
    private String roleId;
    private String customerId;
    private String performedBy;
    private String actionType; // "ASSIGNED" or "REMOVED"
    private LocalDateTime date;
}
