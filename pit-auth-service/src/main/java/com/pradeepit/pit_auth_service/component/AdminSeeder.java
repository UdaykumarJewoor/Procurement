package com.pradeepit.pit_auth_service.component;

import com.pradeepit.pit_auth_service.dto.request.RegistrationRequest;
import com.pradeepit.pit_auth_service.model.Customer;
import com.pradeepit.pit_auth_service.model.Permission;
import com.pradeepit.pit_auth_service.model.Role;
import com.pradeepit.pit_auth_service.model.enums.ActionEnum;
import com.pradeepit.pit_auth_service.model.enums.CustomerStatus;
import com.pradeepit.pit_auth_service.model.enums.StatusEnum;
import com.pradeepit.pit_auth_service.repository.*;
import com.pradeepit.pit_auth_service.util.IdGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Component
@Slf4j
public class AdminSeeder implements ApplicationListener<ContextRefreshedEvent> {

    @Value("${app.master-admin.role}")
    private String masterAdminRoleName;

    @Value("${app.master-admin.fName}")
    private String masterAdminFName;

    @Value("${app.master-admin.lName}")
    private String masterAdminLName;

    @Value("${app.master-admin.email}")
    private String masterAdminEmail;

    @Value("${app.master-admin.phone}")
    private String masterAdminPhone;

    @Value("${app.master-admin.password}")
    private String masterAdminPassword;

    private static final List<String> MODULES
            = List.of("customers", "permissions", "roles", "candidates", "clients", "jobs", "spocs", "userinfo", "branches", "job-assignments");


    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private IdGenerationService idGenerationService;

    /**
     * Application event listener to seed the master administrator after the application context is refreshed.
     *
     * @param contextRefreshedEvent the application event
     */
    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent contextRefreshedEvent) {
        // Seed the master administrator after the application context is refreshed.
        this.createMasterAdministrator();
        this.seedMasterAdminPermissions(roleRepository.findByNameIgnoreCase(masterAdminRoleName).get());
    }

    private void createMasterAdministrator() {
        Optional<Role> optionalRole = roleRepository.findByNameIgnoreCase(masterAdminRoleName);
        if (optionalRole.isEmpty()) {
            throw new IllegalStateException(masterAdminRoleName + "role not found. Please seed roles first.");
        }

        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setFirstName(masterAdminFName);
        registrationRequest.setLastName(masterAdminLName);
        registrationRequest.setEmail(masterAdminEmail);
        registrationRequest.setPassword(masterAdminPassword);

        Optional<Customer> optionalUser = customerRepository.findByEmail(registrationRequest.getEmail());
        if (optionalUser.isPresent()) {
            return;
        }

        Customer user = new Customer();
        user.setId("masterAdmin");
        user.setCustomerNo(idGenerationService.generateCustomerNumber(optionalRole.get().getName()));
        user.setEmail(registrationRequest.getEmail());
        user.setFirstName(masterAdminFName);
        user.setLastName(masterAdminLName);
        user.setPhone(masterAdminPhone);
        user.setPassword(passwordEncoder.encode(masterAdminPassword));
        user.setCustomerStatus(CustomerStatus.Active);
        user.setRole(optionalRole.get());
        user.setRoleName(optionalRole.get().getName());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(null);
        user.setStatus(StatusEnum.Accepted.name());
        log.info("Master admin seeded successfully.");
        customerRepository.save(user);

    }

    private void seedMasterAdminPermissions(Role masterAdminRole) {
        Set<String> masterAdminRoles = Set.of(masterAdminRole.getName());

        MODULES.forEach(module ->
                Stream.of(ActionEnum.values()).forEach(action ->
                        savePermission(module, action, masterAdminRoles)));

    }

    private void savePermission(String module, ActionEnum action, Set<String> masterAdminRoles) {
        Optional<Permission> existingPermission = permissionRepository.findByModuleAndAction(module, action);

        if (existingPermission.isPresent()) {
            return;
        }

        Permission permission = new Permission();
        permission.setId(idGenerationService.generatePermissionId());
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(null);
        permission.setModule(module);
        permission.setAction(action);
        permission.setRoles(masterAdminRoles);

        log.info("Permission for {} {} seeded successfully.", module, action);
        permissionRepository.save(permission);
    }


}
