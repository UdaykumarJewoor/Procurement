package com.pradeepit.pit_auth_service.service;


import com.pradeepit.pit_auth_service.dto.PermissionDTO;
import com.pradeepit.pit_auth_service.dto.PermissionHistoryDTO;
import com.pradeepit.pit_auth_service.mapper.PermissionMapper;
import com.pradeepit.pit_auth_service.model.Permission;
import com.pradeepit.pit_auth_service.model.PermissionHistory;
import com.pradeepit.pit_auth_service.model.Role;
import com.pradeepit.pit_auth_service.model.enums.ActionEnum;
import com.pradeepit.pit_auth_service.repository.PermissionHistoryRepository;
import com.pradeepit.pit_auth_service.repository.PermissionRepository;
import com.pradeepit.pit_auth_service.util.CustomerUserDetailsAdapter;
import com.pradeepit.pit_auth_service.util.IdGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.pradeepit.pit_auth_service.service.AuthenticationService.getCurrentUser;


@Service
@Slf4j
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private IdGenerationService idGenerationService;
    @Autowired
    private PermissionHistoryRepository permissionHistoryRepository;

    /**
     * Create a new permission record.
     *
     * @param permissionDTO the details of the permission to be created
     * @return the created PermissionDTO
     */
    public PermissionDTO createPermission(PermissionDTO permissionDTO) {
        // Generate a new ID for the permission
        permissionDTO.setId(idGenerationService.generatePermissionId());
        // Map the PermissionDTO to a Permission entity
        Permission permission = permissionMapper.dtoToPermission(permissionDTO);
        // Set roles for the permission entity
        permission.setRoles(permissionDTO.getRoles());
        // Save the permission to the repository
        permission = permissionRepository.save(permission);
        // Log the creation event
        log.info("Created new permission with ID: {}", permission.getId());
        // Map the saved Permission entity back to a PermissionDTO and return
        return permissionMapper.permissionToDTO(permission);
    }

    public List<PermissionHistoryDTO> getAllPermissionHistories() {
        return permissionHistoryRepository.findAll()
                .stream()
                .map(permissionMapper::permissionHistoryToDTO)
                .collect(Collectors.toList());
    }


    /**
     * Deletes a permission record from the database.
     *
     * @param id the id of the permission to delete
     * @return true if the permission was found and deleted, false otherwise
     */
    public boolean deletePermission(String id) {
        // Check if the permission exists by its id
        if (permissionRepository.existsById(id)) {
            // Delete the permission if found
            permissionRepository.deleteById(id);
            // Log the deletion event
            log.info("Deleted permission with ID: {}", id);
            // Return true to indicate that the permission was found and deleted
            return true;
        }
        // Log a warning if the permission was not found
        log.warn("Failed to delete. No permission found with ID: {}", id);
        // Return false to indicate that the permission was not found
        return false;
    }

    /**
     * Retrieves a single permission record by its id.
     *
     * @param id the id of the permission to retrieve
     * @return the PermissionDTO if found, null otherwise
     */
    public PermissionDTO getPermissionById(String id) {
        // Attempt to find the permission by its id in the repository
        Optional<Permission> permission = permissionRepository.findByIdIgnoreCase(id);
        // If found, map the Permission entity to a PermissionDTO and return
        return permission.map(permissionMapper::permissionToDTO)
                // If not found, log a warning and return null
                .orElseGet(() -> {
                    log.warn("No permission found with ID: {}", id);
                    return null;
                });
    }

    /**
     * Retrieves all permissions from the repository.
     *
     * @return a list of PermissionDTOs representing all permissions
     */
    public List<PermissionDTO> getAllPermissions() {
        // Fetch all permissions from the repository and map them to DTOs
        List<PermissionDTO> permissions = permissionRepository.findAll().stream()
                .map(permissionMapper::permissionToDTO)
                .toList();

        // Log the number of permissions retrieved
        log.info("Retrieved {} permissions from the database", permissions.size());

        // Return the list of PermissionDTOs
        return permissions;
    }


    /**
     * Checks if the user has the specified permission. If the permission does not exist for the given module and action,
     * the method returns false. If the permission exists and the user has the required role, the method returns true.
     * <p>
     * If the permission does not exist, a warning is logged. If the user does not have the required role, a warning is
     * logged. If the permission exists and the user has the required role, an info log is written.
     *
     * @param userRoles the roles of the user
     * @param module    the module of the permission
     * @param action    the action of the permission
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(Set<String> userRoles, String module, String action) {
        log.debug("Checking permissions for Module: {}, Action: {}, User Roles: {}", module, action, userRoles);

        // Attempt to convert the action to an ActionEnum
        ActionEnum actionEnum;
        try {
            actionEnum = ActionEnum.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid action type: {}", action);
            return false;
        }

        // Attempt to find the permission by module and action
        Optional<Permission> permissionOpt = permissionRepository.findByModuleAndAction(module, actionEnum);

        if (permissionOpt.isPresent()) {
            Permission permission = permissionOpt.get();
            // Check if the user has any of the required roles
            boolean hasPermission = userRoles.stream()
                    .anyMatch(permission.getRoles()::contains);

            if (hasPermission) {
                // Log that the permission was granted
                log.info("Permission granted for Module: {}, Action: {}, Roles: {}", module, action, userRoles);
            } else {
                // Log that the user does not have the required roles
                log.warn("Roles {} do not match required roles for Module: {}, Action: {}", userRoles, module, action);
            }
            return hasPermission;
        }

        // Log that the permission was not found
        log.info("No permission found for Module: {}, Action: {}", module, action);
        return false;
    }

    /**
     * Assigns a permission to a role. This method checks if the permission exists,
     * and if so, adds the role to the permission's associated roles. A PermissionHistory
     * record is created to track the assignment.
     *
     * @param role       the role to assign the permission to
     * @param permission the permission to assign
     * @return the updated permission as a PermissionDTO
     * @throws IllegalArgumentException if the permission is not found
     */
    public PermissionDTO assignPermissionToRole(Role role, Permission permission) {
        // Get the current user details
        UserDetails currentUser = getCurrentUser();

        String customerId;
        if (currentUser instanceof CustomerUserDetailsAdapter) {
            customerId = ((CustomerUserDetailsAdapter) currentUser).getId();
        } else {
            throw new IllegalStateException("Current user is not an instance of CustomerUserDetailsAdapter");
        }

        // Get the performed by username
        String performedBy = currentUser.getUsername();

        // Assign the permission to the role
        permission.getRoles().add(role.getName());
        permissionRepository.save(permission);  // Save the updated permission

        // Create a new PermissionHistory record for the assignment action
        PermissionHistory history = new PermissionHistory(
                idGenerationService.generatePermissionHistoryId(),
                permission.getId(),
                role.getId(),
                customerId,
                performedBy,
                "ASSIGNED",
                LocalDateTime.now()
        );
        permissionHistoryRepository.save(history);  // Save the history

        // Log the assignment action
        log.info("Assigned permission [{}] to role [{}] by [{}]", permission.getId(), role.getId(), performedBy);

        // Return the updated permission as a DTO
        return permissionMapper.permissionToDTO(permission);
    }


    /**
     * Removes a permission from a role. This method checks if the permission exists,
     * and if so, removes the role from the permission's associated roles. A PermissionHistory
     * record is created to track the removal.
     *
     * @param role       the role to assign the permission to
     * @param permission the permission to assign
     * @return the updated permission as a PermissionDTO
     * @throws IllegalArgumentException if the permission is not found
     */
    public PermissionDTO removePermissionFromRole(Role role, Permission permission) {
        // Get the current user details
        UserDetails currentUser = getCurrentUser();
        String customerId;
        if (currentUser instanceof CustomerUserDetailsAdapter) {
            customerId = ((CustomerUserDetailsAdapter) currentUser).getId();
        } else {
            throw new IllegalStateException("Current user is not an instance of CustomerUserDetailsAdapter");
        }
        // Remove the role from the permission's roles
        permission.getRoles().remove(role.getName());
        permissionRepository.save(permission);  // Save the updated permission

        // Create a new PermissionHistory record for the removal action
        PermissionHistory history = new PermissionHistory(
                idGenerationService.generatePermissionHistoryId(),
                permission.getId(),
                role.getId(),
                customerId,
                currentUser.getUsername(),
                "REMOVED",
                LocalDateTime.now()
        );
        permissionHistoryRepository.save(history);  // Save the history

        // Log the removal action
        log.info("Removed permission [{}] from role [{}] by [{}]", permission.getId(), role.getId(), currentUser.getUsername());

        // Return the updated permission as a DTO
        return permissionMapper.permissionToDTO(permission);
    }


    public Map<String, Set<String>> getModulesAndActionsForRole(Role role) {
        String roleName = role.getName(); // Extract role name

        // Fetch permissions for this role
        List<Permission> permissions = permissionRepository.findByRolesContaining(roleName);

        // Map to store Module -> Set of Actions
        Map<String, Set<String>> moduleActionsMap = new HashMap<>();

        for (Permission permission : permissions) {
            String module = permission.getModule();
            String action = permission.getAction().name(); // Convert Enum to String

            // Add action to the corresponding module
            moduleActionsMap
                    .computeIfAbsent(module, k -> new HashSet<>())
                    .add(action);
        }

        log.info("Role [{}] has the following permissions: {}", roleName, moduleActionsMap);
        return moduleActionsMap;
    }

    public List<PermissionDTO> getPermissionsByRole(Role role) {
        List<Permission> permissions = permissionRepository.findByRolesContaining(role.getName());
        return permissions.stream()
                .map(permissionMapper::permissionToDTO)
                .collect(Collectors.toList());
    }

}
