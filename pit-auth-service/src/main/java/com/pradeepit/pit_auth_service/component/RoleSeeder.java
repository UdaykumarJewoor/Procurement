package com.pradeepit.pit_auth_service.component;


import com.pradeepit.pit_auth_service.model.Role;
import com.pradeepit.pit_auth_service.repository.RoleRepository;
import com.pradeepit.pit_auth_service.util.IdGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class RoleSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);

    @Value("${app.master-admin.role}")
    private String masterAdminRoleName;

    @Value("${app.master-admin.roleType}")
    private String masterAdminRoleType;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private IdGenerationService idGenerationService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        seedSuperAdminRole();
        seedAdditionalRoles();
    }

    /**
     * Seeds the SUPER_ADMIN role if it does not exist.
     */
    public void seedSuperAdminRole() {
        Optional<Role> existingRole = roleRepository.findByNameIgnoreCase(masterAdminRoleName);
        if (existingRole.isPresent()) {
            log.info("Super Admin role '{}' already exists. Skipping seeding.", masterAdminRoleName);
            return;
        }

        Role superAdminRole = new Role();
        superAdminRole.setId(idGenerationService.generateRoleId()); // Generate ID only for new role
        superAdminRole.setName(masterAdminRoleName);
        superAdminRole.setRoleType(masterAdminRoleType);
        superAdminRole.setCreatedAt(LocalDateTime.now());
        superAdminRole.setUpdatedAt(null);

        roleRepository.save(superAdminRole);
        log.info("Super Admin role '{}' seeded successfully.", masterAdminRoleName);
    }

    /**
     * Seeds additional roles after the SUPER_ADMIN role has been stored.
     */
    private void seedAdditionalRoles() {
        List<Role> roles = List.of(
                new Role(null, "ADMIN", "Manager", null, null),
                new Role(null, "GUEST-USER", "Visitor", null, null),
                new Role(null, "LEAD-HR", "Lead Recruiter", null, null),
                new Role(null, "RECRUITER", "Recruiter", null, null),
                new Role(null, "CLIENT", "Client", null, null)
        );

        for (Role role : roles) {
            Optional<Role> existingRole = roleRepository.findByNameIgnoreCase(role.getName());
            if (existingRole.isPresent()) {
                log.info("Role '{}' already exists. Skipping seeding.", role.getName());
                continue;
            }

            // Assign a new ID only if the role is new
            role.setId(idGenerationService.generateRoleId());
            role.setCreatedAt(LocalDateTime.now());
            role.setUpdatedAt(null);

            roleRepository.save(role);
            log.info("Role '{}' seeded successfully.", role.getName());
        }
    }
}
