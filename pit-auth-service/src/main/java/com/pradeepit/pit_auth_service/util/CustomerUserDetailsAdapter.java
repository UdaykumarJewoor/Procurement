package com.pradeepit.pit_auth_service.util;

import com.pradeepit.pit_auth_service.model.Customer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomerUserDetailsAdapter implements UserDetails {

    private static final Logger log = LoggerFactory.getLogger(CustomerUserDetailsAdapter.class); // Create logger instance

    @Autowired
    private final Customer customer;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (customer.getRole() == null || customer.getRole().getName() == null) {
            log.warn("Customer has no role assigned: {}", customer.getId());
            return List.of();
        }
        log.debug("Customer authorities: {}", customer.getRole().getName());
        return List.of(() -> customer.getRole().getName());
    }

    @Override
    public String getPassword() {
        return customer.getPassword();
    }

    @Override
    public String getUsername() {
        return customer.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Customize as needed
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Customize as needed
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Customize as needed
    }

    @Override
    public boolean isEnabled() {
        return "Accepted".equalsIgnoreCase(customer.getStatus());
    }

    public String getId() {
        if (customer.getId() == null) {
            throw new IllegalStateException("Customer ID is null. Cannot retrieve ID.");
        }
        String id = customer.getId();
        log.debug("Fetching customer ID: {}", id);
        return customer.getId();
    }

}
