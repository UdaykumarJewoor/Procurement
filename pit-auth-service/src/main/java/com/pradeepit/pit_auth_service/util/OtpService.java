package com.pradeepit.pit_auth_service.util;


import com.pradeepit.pit_auth_service.model.Customer;
import com.pradeepit.pit_auth_service.repository.CustomerRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class); // Create logger instance

    @Value("${otp.expiry-seconds}")
    private int otpExpirySeconds;

    @Value("${otp.check-interval}")
    private int checkIntervalSeconds;

    @Autowired
    private CustomerRepository customerRepository;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void initializeServices() {
        startAutoMaintenanceTask();
    }

    private void startAutoMaintenanceTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndUnfreezeAccounts();
                expireOtp();
            } catch (Exception e) {
                log.error("Error in maintenance task: {}", e.getMessage());
            }
        }, 0, checkIntervalSeconds, TimeUnit.SECONDS);
    }

    private void expireOtp() {
        List<Customer> customersWithOtp = customerRepository.findAllByOtpIsNotNull();

        for (Customer customer : customersWithOtp) {
            if (customer.getOtpExpiredAt() != null && customer.getOtpExpiredAt().isBefore(LocalDateTime.now())) {
                customer.setOtp(null);
                customer.setOtpGeneratedAt(null);
                customer.setOtpExpiredAt(null);
                customerRepository.save(customer);
                log.info("Expired OTP for email: {}", customer.getEmail());
            }
        }
    }

    private void checkAndUnfreezeAccounts() {
        List<Customer> frozenCustomers = customerRepository.findAllByIsFrozen(true);

        for (Customer customer : frozenCustomers) {
            LocalDateTime frozenUntil = customer.getFrozenUntil();
            if (frozenUntil != null && frozenUntil.isBefore(LocalDateTime.now())) {
                unfreezeAccount(customer);
            }
        }
    }

    public void unfreezeAccount(Customer customer) {
        customer.setFrozen(false);
        customer.setOtp(null);
        customer.setLoginAttempts(0);
        customer.setOtpAttempts(0);
        customer.setFrozenUntil(null);
        customer.setOtpGeneratedAt(null);
        customer.setOtpExpiredAt(null);
        customerRepository.save(customer);

        log.info("Account for email {} has been unfrozen.", customer.getEmail());
    }

    public String generateOtp(String email) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isEmpty()) {
            log.error("No customer found for email: {}", email);
            return "Customer not found.";
        }

        Customer customer = optionalCustomer.get();

        if (isAccountFrozen(customer)) {
            log.warn("Cannot generate OTP. Account is frozen for email: {}", email);
            return "Account is frozen. Try again later.";
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime now = LocalDateTime.now();

        customer.setOtp(otp);
        customer.setOtpGeneratedAt(now);
        customer.setOtpExpiredAt(now.plusSeconds(otpExpirySeconds));
        customerRepository.save(customer);
        customer.setLoginAttempts(0);
        customer.setOtpAttempts(0); // Reset attempts on new OTP
        customerRepository.save(customer);

        log.info("Generated OTP: {} for email: {}", otp, email);
        return otp;
    }

    private boolean isAccountFrozen(Customer customer) {
        return customer.getFrozenUntil() != null && customer.getFrozenUntil().isAfter(LocalDateTime.now());
    }
}

