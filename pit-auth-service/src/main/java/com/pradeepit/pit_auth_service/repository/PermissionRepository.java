package com.pradeepit.pit_auth_service.repository;


import com.pradeepit.pit_auth_service.model.Permission;
import com.pradeepit.pit_auth_service.model.enums.ActionEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {
    Optional<Permission> findByModuleAndAction(String module, ActionEnum action);

    @Query("{ 'id' : { $regex: ?0, $options: 'i' } }")
    Optional<Permission> findByIdIgnoreCase(String permissionId);

    List<Permission> findByRolesContaining(String roleName);

}
