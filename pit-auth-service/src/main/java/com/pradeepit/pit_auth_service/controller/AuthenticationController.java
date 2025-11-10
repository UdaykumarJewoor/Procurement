package com.pradeepit.pit_auth_service.controller;

import com.pradeepit.pit_auth_service.dto.request.LoginRequest;
import com.pradeepit.pit_auth_service.dto.request.LoginRequestOTP;
import com.pradeepit.pit_auth_service.dto.request.RegistrationRequest;
import com.pradeepit.pit_auth_service.dto.response.LoginResponse;
import com.pradeepit.pit_auth_service.dto.response.RegistrationResponse;
import com.pradeepit.pit_auth_service.model.Customer;
import com.pradeepit.pit_auth_service.model.Role;
import com.pradeepit.pit_auth_service.model.enums.StatusEnum;
import com.pradeepit.pit_auth_service.repository.*;
import com.pradeepit.pit_auth_service.service.*;
import com.pradeepit.pit_auth_service.util.CustomerUserDetailsAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.pradeepit.pit_auth_service.service.AuthenticationService.getCurrentUser;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleRepository roleRepository;
    @Value("${roles.GUEST-USER}")
    private String guestRole;
    @Value("${otp.freeze-timeout}")
    private int freezeTimeout;
    @Value("${otp.max-attempts}")
    private int maxOtpAttempts;

    /**
     * Helper method to generate time left message
     */
    private String getTimeLeftMessage(Duration duration) {
        long minutesLeft = duration.toMinutes();
        long secondsLeft = duration.getSeconds() % 60;

        return minutesLeft > 0
                ? minutesLeft + " minutes and " + secondsLeft + " seconds"
                : secondsLeft + " seconds";
    }


    @GetMapping("/me/id")
    public String getCustomerId() {
        UserDetails currentUser = getCurrentUser();

        String customerId;
        if (currentUser instanceof CustomerUserDetailsAdapter) {
            customerId = ((CustomerUserDetailsAdapter) currentUser).getId();
        } else {
            return "Current user is not an instance of CustomerUserDetailsAdapter";
        }
        return customerId;
    }

    /**
     * Logs in a user.
     *
     * @param request the authentication request
     * @return ResponseEntity with login status and token
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String message = "Login Successfully Completed.";
            Optional<Customer> optionalCustomer = customerRepository.findByEmail(request.getEmail());

            if (optionalCustomer.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "A user with the provided " + request.getEmail() + " address does not exist in our records."));
            }

            Customer customer = optionalCustomer.get();


            if (StatusEnum.Declined.name().equalsIgnoreCase(customer.getStatus())) {
                log.info("Login attempt for declined account: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Your account has been declined. Please contact the admin."));
            } else if (StatusEnum.Pending.name().equalsIgnoreCase(customer.getStatus())) {
                log.info("Login attempt for pending account: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Your account has not been verified by the administrator yet. You will be notified by email once it is verified."));
            }

            // Check if the account is frozen
            if (customer.isFrozen() && customer.getFrozenUntil() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(customer.getFrozenUntil())) {
                    String timeLeftMessage = getTimeLeftMessage(Duration.between(now, customer.getFrozenUntil()));
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Your account is frozen. Try again after " + timeLeftMessage + "."));
                } else {
                    // Unfreeze the account after the timeout
                    customer.setFrozen(false);
                    customer.setLoginAttempts(0);  // Reset login attempts
                    customer.setOtpAttempts(0);
                    customer.setFrozenUntil(null);
                    customerRepository.save(customer);
                }
            }

            // Check if the entered password is correct
            if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
                log.info("Invalid password for email: {}", request.getEmail());

                // Increment login attempts counter
                customer.setLoginAttempts(customer.getLoginAttempts() + 1);

                int remainingAttempts = maxOtpAttempts - customer.getLoginAttempts();  // Calculate remaining attempts

                if (customer.getLoginAttempts() >= maxOtpAttempts) {
                    // Freeze the account after 3 failed login attempts
                    customer.setFrozen(true);
                    customer.setFrozenUntil(LocalDateTime.now().plusMinutes(freezeTimeout));  // Frozen for 30 minutes (customize as needed)
                    customerRepository.save(customer);
                    log.info("Account frozen due to too many failed login attempts for email: {}", request.getEmail());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Your account is frozen due to too many failed login attempts. Try again after " + getTimeLeftMessage(Duration.between(LocalDateTime.now(), customer.getFrozenUntil())) + "."));
                } else {
                    customerRepository.save(customer);  // Save the updated login attempts
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Incorrect username or password. You have " + remainingAttempts + " attempts remaining."));
                }
            }

//            Optional<UserInfo> optionalUserProfile = userRepository.findByCustomerIdIgnoreCase(customer.getId());
//
//            if (optionalUserProfile.isPresent()) {
//                optionalUserProfile.get().setProfileCompleted(true);
//            } else {
//                // Create an empty profile if not present
//                UserInfo newUserProfile = new UserInfo();
//                newUserProfile.setId(idGenerationService.generateUserId());
//                newUserProfile.setCustomerId(customer.getId());
//                newUserProfile.setRole(customer.getRoleName());
//                newUserProfile.setProfileCompleted(false);
//                newUserProfile.setCreatedAt(LocalDateTime.now());
//                newUserProfile.setUpdatedAt(null);
//                userRepository.save(newUserProfile);
//            }

            LoginResponse loginResponse = authenticationService.login(request);
            return ResponseEntity.ok(Map.of(
                    "message", message,
                    "data", loginResponse
            ));
        } catch (RuntimeException ex) {
            log.error("Login error: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * Registers a new user.
     *
     * @param request the registration request
     * @return ResponseEntity with registration status and data
     */
    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        try {
            Optional<Role> roleOptional = roleRepository.findByNameIgnoreCase(guestRole);
            if (roleOptional.isEmpty()) {
                log.info("Role '{}' not found! Please seed roles first.", guestRole);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Role with " + guestRole + " not found, please check system configuration."));
            }

            Role role = roleOptional.get();

            if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
                log.info("Email '{}' is already registered!", request.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Email " + request.getEmail() + " is already registered!"));
            }
            RegistrationResponse response = authenticationService.registerCustomer(request, role);

            if (response != null) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                                "message", "Registration Successfully Completed",
                                "data", response
                        ));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Registration Failed"));
        } catch (RuntimeException ex) {
            log.error("Registration error: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));
        }
    }


    /**
     * Initiates login by verifying email and password, then sending OTP.
     *
     * @param request the authentication request
     * @return ResponseEntity with OTP status
     */
    @PostMapping("/login/initiate")
    public ResponseEntity<?> initiateLogin(@RequestBody LoginRequest request) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(request.getEmail());

        if (optionalCustomer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "A user with the provided " + request.getEmail() + " address does not exist in our records."));
        }

        Customer customer = optionalCustomer.get();

        // Check account status
        if (StatusEnum.Declined.name().equalsIgnoreCase(customer.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Your account has been declined. Please contact the admin."));
        } else if (StatusEnum.Pending.name().equalsIgnoreCase(customer.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Your account has not been verified by the administrator yet. You will be notified by email once it is verified."));
        }


        // Check if the account is frozen
        if (customer.isFrozen() && customer.getFrozenUntil() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(customer.getFrozenUntil())) {
                String timeLeftMessage = getTimeLeftMessage(Duration.between(now, customer.getFrozenUntil()));
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Your account is frozen. Try again after " + timeLeftMessage + "."));
            } else {
                // Unfreeze the account after the timeout
                customer.setFrozen(false);
                customer.setLoginAttempts(0);  // Reset login attempts
                customer.setOtpAttempts(0);
                customer.setFrozenUntil(null);
                customerRepository.save(customer);
            }
        }

        // Check if the entered password is correct
        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            log.info("Invalid password for email: {}", request.getEmail());

            // Increment login attempts counter
            customer.setLoginAttempts(customer.getLoginAttempts() + 1);

            int remainingAttempts = maxOtpAttempts - customer.getLoginAttempts();  // Calculate remaining attempts

            if (customer.getLoginAttempts() >= maxOtpAttempts) {
                // Freeze the account after 3 failed login attempts
                customer.setFrozen(true);
                customer.setFrozenUntil(LocalDateTime.now().plusMinutes(freezeTimeout));  // Frozen for 30 minutes (customize as needed)
                customerRepository.save(customer);
                log.info("Account frozen due to too many failed login attempts for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Your account is frozen due to too many failed login attempts. Try again after " + getTimeLeftMessage(Duration.between(LocalDateTime.now(), customer.getFrozenUntil())) + "."));
            } else {
                customerRepository.save(customer);  // Save the updated login attempts
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Incorrect username or password. You have " + remainingAttempts + " attempts remaining."));
            }
        }

        // If authentication is successful, proceed with OTP generation
        if (customer.getOtp() != null && customer.getOtpExpiredAt() != null &&
                customer.getOtpExpiredAt().isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "OTP already sent. Please wait " + getTimeLeftMessage(Duration.between(LocalDateTime.now(), customer.getOtpExpiredAt())) + " until it expires before requesting a new one."));
        }

        authenticationService.initiateLogin(request);

        return ResponseEntity.ok(Map.of("message", "OTP has been sent to your email."));
    }

    /**
     * Verifies OTP and completes login by issuing a JWT token.
     *
     * @param loginRequest the login request with email and OTP
     * @return ResponseEntity with login token
     */
    @PostMapping("/login/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody LoginRequestOTP loginRequest) {
        try {
            String email = loginRequest.getEmail();
            String otp = loginRequest.getOtp();

            // Find customer by email
            Customer customer = authenticationService.findCustomerByEmail(email);
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found"));
            }

            // Check if the account is frozen
            if (customer.isFrozen() && customer.getFrozenUntil() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(customer.getFrozenUntil())) {
                    String timeLeftMessage = getTimeLeftMessage(Duration.between(now, customer.getFrozenUntil()));
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Your account is frozen. Try again after " + timeLeftMessage + "."));
                } else {
                    // Unfreeze the account after the timeout
                    customer.setFrozen(false);
                    customer.setOtpAttempts(0);
                    customer.setFrozenUntil(null);
                    customerRepository.save(customer);
                }
            }

            // Define max allowed attempts
            int maxAttempts = maxOtpAttempts;

            // Perform OTP verification
            if (!authenticationService.isOtpValid(email, otp)) {
                customer.setOtpAttempts(customer.getOtpAttempts() + 1);

                if (customer.getOtpAttempts() >= maxAttempts) {
                    customer.setFrozen(true);
                    customer.setFrozenUntil(LocalDateTime.now().plusMinutes(freezeTimeout));
                    customer.setOtp(null);
                    customerRepository.save(customer);

                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Your account has been frozen due to multiple failed login attempts. Try again after " + getTimeLeftMessage(Duration.between(LocalDateTime.now(), customer.getFrozenUntil())) + "."));
                }

                customerRepository.save(customer);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid OTP. You have " + (maxAttempts - customer.getOtpAttempts()) + " attempts left."));
            }

//            Optional<UserInfo> optionalUserProfile = userRepository.findByCustomerIdIgnoreCase(customer.getId());
//
//            if (optionalUserProfile.isPresent()) {
//                optionalUserProfile.get().setProfileCompleted(true);
//            } else {
//                // Create an empty profile if not present
//                UserInfo newUserProfile = new UserInfo();
//                newUserProfile.setId(idGenerationService.generateUserId());
//                newUserProfile.setCustomerId(customer.getId());
//                newUserProfile.setRole(customer.getRoleName());
//                newUserProfile.setProfileCompleted(false);
//                newUserProfile.setCreatedAt(LocalDateTime.now());
//                newUserProfile.setUpdatedAt(null);
//                userRepository.save(newUserProfile);
//            }

            // Reset OTP attempts and login
            customer.setOtpAttempts(0);
            customer.setFrozen(false);
            customer.setFrozenUntil(null);
            customerRepository.save(customer);

            LoginResponse loginResponse = authenticationService.verifyOtpAndLogin(email, otp);
            return ResponseEntity.ok(Map.of("message", "Login successful", "data", loginResponse));

        } catch (RuntimeException ex) {
            log.error("OTP verification error: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

}
