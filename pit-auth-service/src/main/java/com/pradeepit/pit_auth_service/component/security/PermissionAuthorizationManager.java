package com.pradeepit.pit_auth_service.component.security;

import com.pradeepit.pit_auth_service.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class PermissionAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Logger logger = LoggerFactory.getLogger(PermissionAuthorizationManager.class); // Create logger instance

    @Autowired
    private PermissionService permissionService;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get(); // Get authentication object

        // Log if the user is not authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("User is not authenticated or authentication is null");
            throw new AccessDeniedException("User is not authenticated");
        }

        String urlPattern = context.getRequest().getRequestURI();
        logger.info("Checking permission for URL: {}", urlPattern);

        String[] moduleAction = extractModuleActionFromUrl(urlPattern);

        // Log if the URL structure is invalid
        if (moduleAction == null) {
            logger.error("Invalid URL structure for permission check: {}", urlPattern);
            throw new AccessDeniedException("Invalid permission request");
        }

        String module = moduleAction[0];
        String action = moduleAction[1];

        logger.info("Extracted module: {}, action: {}", module, action);

        Set<String> userRoles = getUserRoles(authentication);
        logger.info("User roles: {}", userRoles);

        boolean hasPermission = permissionService.hasPermission(userRoles, module, action);

        if (!hasPermission) {
            logger.warn("User does NOT have permission for module: {}, action: {}", module, action);
            throw new AccessDeniedException("Access Denied: You do not have permission to perform this action");
        }

        return new AuthorizationDecision(true);
    }

    private String[] extractModuleActionFromUrl(String urlPattern) {
        if (urlPattern.startsWith("/api/")) {
            String[] parts = urlPattern.split("/");

            // Ensure correct URL structure (we need at least "/api/{module}/{action}")
            if (parts.length >= 4) {
                String module = parts[2];  // The module name (e.g., CustomerModule)
                String action = parts[3].toUpperCase();  // The action (CREATE, READ, etc.)

                // Validate if action matches expected actions (CREATE, READ, UPDATE, DELETE)
                if (action.equalsIgnoreCase("CREATE") ||
                        action.equalsIgnoreCase("READ") ||
                        action.equalsIgnoreCase("UPDATE") ||
                        action.equalsIgnoreCase("DELETE")) {
                    return new String[]{module, action};
                }
            }
        }
        return null;  // Return null if URL structure is incorrect or action is invalid
    }


    private Set<String> getUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
