package com.pradeepit.pit_auth_service.service;

import com.pradeepit.pit_auth_service.component.security.JwtUtil;
import com.pradeepit.pit_auth_service.dto.request.LoginRequest;
import com.pradeepit.pit_auth_service.dto.request.RegistrationRequest;
import com.pradeepit.pit_auth_service.dto.response.LoginResponse;
import com.pradeepit.pit_auth_service.dto.response.RegistrationResponse;
import com.pradeepit.pit_auth_service.mapper.CustomerMapper;
import com.pradeepit.pit_auth_service.model.Customer;
import com.pradeepit.pit_auth_service.model.Role;
import com.pradeepit.pit_auth_service.model.enums.StatusEnum;
import com.pradeepit.pit_auth_service.repository.*;
import com.pradeepit.pit_auth_service.util.CustomerUserDetailsAdapter;
import com.pradeepit.pit_auth_service.util.EmailService;
import com.pradeepit.pit_auth_service.util.IdGenerationService;
import com.pradeepit.pit_auth_service.util.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class AuthenticationService {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private OtpService otpService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private CustomerMapper customerMapper;
    @Autowired
    private IdGenerationService idGenerationService;
    @Value("${otp.expiry-seconds}")
    private int otpExpirySeconds;
    @Value("${roles.GUEST-USER}")
    private String guestRole;

    /**
     * Generates a JWT token for the provided customer.
     *
     * @param customer the customer for which to generate the token
     * @return the generated JWT token
     */
    public String generateToken(Customer customer) {
        // Create a map of claims to include in the token, which includes the user's role
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", customer.getRole().getName());

        // Use the JwtUtil to generate the token with the given claims and the customer's details
        return jwtUtil.generateToken(claims, new CustomerUserDetailsAdapter(customer));
    }


    /**
     * Registers a new user in the system.
     *
     * @param request the user registration request
     * @return a registration response containing the newly registered user's data
     */
    public RegistrationResponse registerCustomer(RegistrationRequest request, Role role) {
        // Map the request to a customer entity
        Customer customer = customerMapper.toEntity(request);
        customer.setId(idGenerationService.generateCustomerId());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setCustomerNo(idGenerationService.generateCustomerNumber(role.getName()));
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setRole(role);
        customer.setRoleName(role.getName());
        customer.setStatus(StatusEnum.Pending.name());
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(null);
        // Save the customer to the database
        customerRepository.save(customer);

        // Return the customer as a response
        return customerMapper.toResponse(customer);
    }

    public List<RegistrationResponse> registerCustomers(List<RegistrationRequest> requests, Role role) {
        List<RegistrationResponse> responses = new ArrayList<>();

        for (RegistrationRequest request : requests) {

            // Map the request to a customer entity
            Customer customer = customerMapper.toEntity(request);
            customer.setId(idGenerationService.generateCustomerId());
            customer.setCustomerNo(idGenerationService.generateCustomerNumber(role.getName()));
            customer.setFirstName(request.getFirstName());
            customer.setLastName(request.getLastName());
            customer.setPassword(passwordEncoder.encode(request.getPassword()));
            customer.setRole(role);
            customer.setRoleName(role.getName());
            customer.setStatus(StatusEnum.Pending.name());
            customer.setCreatedAt(LocalDateTime.now());
            customer.setUpdatedAt(null);

            // Save the customer to the database
            customerRepository.save(customer);

            // Add to the response list
            responses.add(customerMapper.toResponse(customer));
        }

        return responses;
    }


    public LoginResponse login(LoginRequest request) {
        Customer customer = findCustomerByEmail(request.getEmail());

        // Check if the account is frozen
        if (customer.isFrozen() && customer.getFrozenUntil() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(customer.getFrozenUntil())) {
                log.info("Account is frozen until: {}", customer.getFrozenUntil());
                return null;
            } else {
                // Unfreeze the account after the timeout
                customer.setFrozen(false);
                customer.setLoginAttempts(0);  // Reset login attempts
                customer.setFrozenUntil(null);
                customerRepository.save(customer);
            }
        }

        String customMessage = getCustomMessage(customer);

        // Generate a new JWT token for the user
        String newToken = generateToken(customer);
        customer.setLoginAttempts(0);  // Reset login attempts
        customer.setAccessToken(newToken);
        customerRepository.save(customer);

        return new LoginResponse(newToken, customMessage);
    }


    /**
     * Initiates the login process for a user.
     * <p>
     * This method verifies the email and password for the user and generates a one-time password (OTP) if both are valid.
     * The OTP is sent to the user's email address.
     * <p>
     * If the account is declined or pending, this method throws an exception.
     * <p>
     * If the account is frozen, the OTP is not sent to the user.
     *
     * @param request the login request containing the email and password
     */
    public void initiateLogin(LoginRequest request) {
        // Find the customer by their email
        Customer customer = findCustomerByEmail(request.getEmail());

        // Check if the account is frozen
        if (customer.isFrozen() && customer.getFrozenUntil() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(customer.getFrozenUntil())) {
                log.info("Account is frozen until: {}", customer.getFrozenUntil());
                return; // Account is frozen
            } else {
                // Unfreeze the account after the timeout
                customer.setFrozen(false);
                customer.setLoginAttempts(0);  // Reset login attempts
                customer.setFrozenUntil(null);
                customerRepository.save(customer);
            }
        }

        // Generate and send OTP if necessary
        customer.setLoginAttempts(0);  // Reset login attempts
        customerRepository.save(customer);
        String otp = otpService.generateOtp(request.getEmail());
        log.info("OTP {} generated for email: {}", otp, request.getEmail());

        if (!customer.isFrozen()) {
            sendOtpEmail(customer, otp);
        }

    }


    /**
     * Verifies the OTP and completes the login process by issuing a JWT token.
     * <p>
     * This method checks if the OTP associated with the email is valid and not expired.
     * If the OTP is valid, it is removed from the OTP store to prevent reuse.
     * <p>
     * The method then generates a JWT token for the user and sets it as the access token.
     * Finally, the method returns a login response containing the JWT token and a custom message.
     *
     * @param email the email to verify the OTP for
     * @param otp   the OTP to be verified
     * @return a login response containing the JWT token and a custom message
     */
    public LoginResponse verifyOtpAndLogin(String email, String otp) {

        // Find the customer by email
        Customer customer = findCustomerByEmail(email);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found.");
        }

        // Verify OTP expiration and validity
        if (customer.getOtpExpiredAt() != null && LocalDateTime.now().isAfter(customer.getOtpExpiredAt())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        // Validate OTP
        if (!customer.getOtp().equals(otp)) {
            // Simply throw an exception without incrementing OTP attempts
            throw new IllegalArgumentException("Invalid OTP.");
        }

        // OTP is valid, reset OTP attempts
        customer.setOtpAttempts(0);  // Reset the OTP attempts if valid
        customer.setOtp(null); // Clear OTP
        customer.setOtpExpiredAt(null); // Clear OTP expiration time
        customer.setOtpGeneratedAt(null); // Clear OTP generation time
        customerRepository.save(customer);

        // Generate a JWT token for the user
        String token = generateToken(customer);

        // Set the access token for the customer
        customer.setAccessToken(token);
        customerRepository.save(customer);

        // Return the JWT token and a custom message (you can define this message based on the user)
        String customMessage = getCustomMessage(customer);

        return new LoginResponse(token, customMessage);
    }


    /**
     * Sends an email to the user containing the OTP for account verification.
     *
     * @param customer the customer object containing the user's email address
     */
    private void sendOtpEmail(Customer customer, String otp) {
        // Create the template data for the email
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customerName", customer.getFirstName() + " " + customer.getLastName());
        templateData.put("customerEmail", customer.getEmail());
        templateData.put("otp", otp);  // Use the OTP passed from initiateLogin
        templateData.put("expiresIn", otpExpirySeconds);

        // Set the subject of the email
        String subject = "Welcome to PradeepIT - Your OTP for Account Verification";

        // Send the email using the email service
        emailService.sendEmail("otp_email_template.ftl", customer.getEmail(), subject, templateData);
    }

    /**
     * Finds a customer by their email address.
     * <p>
     * This method queries the customer repository to find the customer associated with the provided email.
     * If the customer is not found, it throws a RuntimeException with a message indicating that the email is invalid.
     *
     * @param email the email address to search for
     * @return the customer object associated with the email address
     */
    public Customer findCustomerByEmail(String email) {
        Optional<Customer> customerOptional = customerRepository.findByEmail(email);
        if (customerOptional.isEmpty()) {
            log.info("Customer not found with email: {}", email);
            throw new RuntimeException("Invalid email or password");
        }
        return customerOptional.get();
    }

    /**
     * Returns a custom message based on the customer's status.
     * <p>
     * If the customer's status is "Pending", the message is a generic message indicating that the account is not yet
     * verified. Otherwise, the message is a welcome message with the customer's name and role.
     *
     * @param customer the customer object
     * @return a custom message
     */
    private String getCustomMessage(Customer customer) {
        // If the customer's status is "Pending", return a message indicating that the account is not yet verified
        if ("Pending".equalsIgnoreCase(customer.getStatus())) {
            return "Your account is not yet verified. Please contact the admin for more details.";
        } else if ("Declined".equalsIgnoreCase(customer.getStatus())) {
            return "Your account has been declined. Please contact the admin for more details.";
        }

        // Otherwise, return a welcome message with the customer's name and role
        String username = customer.getEmail();
        String roleName = customer.getRole().getName();

        return "Welcome, " + username + "! Your role is: " + roleName + ".";
    }

    public static UserDetails getCurrentUser() {
        return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public boolean isOtpValid(String email, String otp) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getOtp() == null) {
            throw new RuntimeException("OTP not generated. Please request a new OTP.");
        }

        // Check if OTP matches and is not expired
        return customer.getOtp().equals(otp) && customer.getOtpExpiredAt().isAfter(LocalDateTime.now());
    }
}
