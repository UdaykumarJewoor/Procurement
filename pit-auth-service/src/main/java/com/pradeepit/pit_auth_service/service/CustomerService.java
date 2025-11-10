package com.pradeepit.pit_auth_service.service;
import com.pradeepit.pit_auth_service.dto.CustomerDTO;
import com.pradeepit.pit_auth_service.dto.request.CreateUserRequest;
import com.pradeepit.pit_auth_service.dto.request.RegistrationRequest;
import com.pradeepit.pit_auth_service.dto.response.RegistrationResponse;
import com.pradeepit.pit_auth_service.mapper.CustomerMapper;
import com.pradeepit.pit_auth_service.model.Customer;
import com.pradeepit.pit_auth_service.model.Role;
import com.pradeepit.pit_auth_service.model.enums.StatusEnum;
import com.pradeepit.pit_auth_service.repository.CustomerRepository;
import com.pradeepit.pit_auth_service.util.EmailService;
import com.pradeepit.pit_auth_service.util.IdGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;
//    @Autowired
//    private ClientRepository clientRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private CustomerMapper customerMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private IdGenerationService idGenerationService;
//    @Autowired
//    private ClientService clientService;



    /**
     * Retrieves all customers.
     */
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customerMapper::convertToDTO)
                .toList();
    }

    /**
     * Retrieves a customer by ID.
     */
    public Optional<CustomerDTO> getCustomerById(String id) {
        return customerRepository.findByIdIgnoreCase(id)
                .map(customerMapper::convertToDTO);
    }

    public boolean isCustomerPresent(String id) {
        Optional<Customer> customerOptional = customerRepository.findByIdIgnoreCase(id);
        if (customerOptional.isEmpty()) {
            log.info("Customer not found with id: {}", id);
            return false;
        }
        return true;
    }


    @Transactional
    public RegistrationResponse createUser(CreateUserRequest request, Role role) {

        Customer customer = customerMapper.toUserEntity(request);
        customer.setId(idGenerationService.generateCustomerId());

        String defaultPassword = "Pradeepit@123";
        customer.setPassword(passwordEncoder.encode(defaultPassword));
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setRole(role);
        customer.setRoleName(role.getName());
        customer.setStatus(StatusEnum.Accepted.name());
        customer.setCreatedAt(LocalDateTime.now());

        customerRepository.save(customer);

        sendEmailToNewUser(customer, defaultPassword);

        return customerMapper.toResponse(customer);
    }

    private RegistrationResponse createClient(RegistrationRequest request, Role role) {
        Customer customer = customerMapper.toClientEntity(request);
        customer.setId(idGenerationService.generateCustomerId());

        String defaultPassword = "Pradeepit@123";
        customer.setPassword(passwordEncoder.encode(defaultPassword));
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setRole(role);
        customer.setRoleName(role.getName());
        customer.setStatus(StatusEnum.Accepted.name());
        customer.setCreatedAt(LocalDateTime.now());

        customerRepository.save(customer);

        sendEmailToNewUser(customer, defaultPassword);

        return customerMapper.toResponse(customer);
    }


    @Transactional
    public List<RegistrationResponse> createUsers(List<CreateUserRequest> requests, Role role) {

        List<RegistrationResponse> responseList = new ArrayList<>();

        for (CreateUserRequest request : requests) {
            if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
                log.info("Email '{}' is already registered. Skipping user creation.", request.getEmail());
                // You can add a response for failed users if needed, or log and continue
                continue;
            }

            Customer customer = customerMapper.toUserEntity(request);
            customer.setId(idGenerationService.generateCustomerId());

            // Set default password
            String defaultPassword = "Pradeepit@123";
            customer.setPassword(passwordEncoder.encode(defaultPassword));

            customer.setRole(role);
            customer.setRoleName(role.getName());
            customer.setStatus(StatusEnum.Accepted.name());
            customer.setCreatedAt(LocalDateTime.now());

            customerRepository.save(customer);

            // Send the email to the new user
            sendEmailToNewUser(customer, defaultPassword);

            // Add the created user response to the response list
            responseList.add(customerMapper.toResponse(customer));
        }

        return responseList;
    }


    private void sendEmailToNewUser(Customer customer, String defaultPassword) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customerId", customer.getId());
        templateData.put("role", customer.getRoleName());
        templateData.put("role_type", customer.getRole().getRoleType() != null ? customer.getRole().getRoleType() : "Guest User");
        templateData.put("status", customer.getStatus());
        templateData.put("customerName", customer.getFirstName() + " " + customer.getLastName());
        templateData.put("customerEmail", customer.getEmail());
        templateData.put("creationDate", formatDateTime(customer.getCreatedAt()));
        templateData.put("defaultPassword", defaultPassword);
        String subject = "Welcome to PradeepIT - Your Account is Created";
        templateData.put("message", "Your account has been created successfully.");
        String template = "account_created.ftl";
        emailService.sendEmail(template, customer.getEmail(), subject, templateData);
    }

    @Transactional
    public void updateCustomerRule(String customerId, StatusEnum newStatus, String newRole,
                                   UserDetails currentUser, Customer customer, Role role) {
        boolean sendStatusEmail = false;
        boolean sendRoleEmail = false;

        // Prevent updating status to Declined if already Declined
        if (StatusEnum.Declined.name().equalsIgnoreCase(newStatus.name()) &&
                StatusEnum.Declined.name().equalsIgnoreCase(customer.getStatus())) {
            log.info("Customer [{}] is already in status [{}], skipping email.", customerId, newStatus.name());
        } else {
            // Update customer status and timestamp
            customer.setStatus(newStatus.name());
            customer.setUpdatedAt(LocalDateTime.now());
            customerRepository.save(customer);
            log.info("Updated status for customer [{}] to [{}] by user [{}]", customerId, newStatus, currentUser.getUsername());

            // Send email if the status is Declined
            if (StatusEnum.Declined.name().equalsIgnoreCase(newStatus.name())) {
                sendStatusEmail = true;
            }
        }

        // Only update role if the new status is not Declined
        if (!StatusEnum.Declined.name().equalsIgnoreCase(newStatus.name()) && newRole != null) {
            String currentRoleName = (customer.getRole() != null) ? customer.getRole().getName() : "No Role Assigned";

            if (!currentRoleName.equals(newRole)) {
                log.info("Updating role for customer [{}] from [{}] to [{}] by user [{}]",
                        customerId, currentRoleName, newRole, currentUser.getUsername());

                // Update the customer's role
                customer.setRole(role);
                customer.setRoleName(newRole);

                customer.setUpdatedAt(LocalDateTime.now());
                customerRepository.save(customer);

                log.info("Updated role for customer [{}] to [{}] by user [{}]", customerId, newRole, currentUser.getUsername());

                // Set flag to send role update email
                sendRoleEmail = true;
            } else {
                log.info("No role update needed for customer [{}] as the role remains [{}]",
                        customerId, currentRoleName);
            }
        }

        // Send email if the status is Declined
        if (sendStatusEmail) {
            Map<String, Object> templateData = buildTemplateData(customer, null);
            emailService.sendEmail("customer_status_update.ftl", customer.getEmail(),
                    "Your Account Status: Declined", templateData);
        }

        // Send email if the role is updated
        if (sendRoleEmail) {
            Map<String, Object> templateData = buildTemplateData(customer, newRole);
            emailService.sendEmail("customer_status_update.ftl", customer.getEmail(),
                    "Your Role Has Been Updated", templateData);
        }
    }


    /**
     * Updates the status of a customer.
     *
     * @param customerId  the ID of the customer to update
     * @param status      the new status to set
     * @param currentUser the user performing the update
     * @throws IllegalArgumentException if the customer is not found
     * @throws SecurityException        if the update is not permitted
     */
    @Transactional
    public void updateCustomerStatus(String customerId, StatusEnum status, UserDetails currentUser, Customer customer) {
        // Prevent updating status to Declined if the customer is already in Declined status
        if (StatusEnum.Declined.name().equalsIgnoreCase(status.name()) &&
                StatusEnum.Declined.name().equalsIgnoreCase(customer.getStatus())) {
            log.info("Customer [{}] is already in status [{}], skipping email.", customerId, status);
            return;
        }

        // Update customer status and timestamp
        customer.setStatus(status.name());
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);
        log.info("Updated status for customer [{}] to [{}] by user [{}]", customerId, status, currentUser.getUsername());

//        // **Update Client Status** when customer status changes
//        Optional<Clients> clientOptional = clientRepository.findByCustomerId(customerId);
//        if (clientOptional.isPresent()) {
//            Clients client = clientOptional.get();
//            client.setStatus(status.name()); // Sync client status with customer status
//            clientRepository.save(client);
//            log.info("Updated client [{}] status to [{}]", client.getId(), status);
//        } else {
//            log.warn("No client found for customer ID [{}]", customerId);
//        }

        // Send email notification if the status is Declined
        if (StatusEnum.Declined.name().equalsIgnoreCase(status.name())) {
            Map<String, Object> templateData = buildTemplateData(customer, null);
            String template = "customer_status_update.ftl";
            emailService.sendEmail(template, customer.getEmail(), "Your Account Status: Declined", templateData);
        }
    }

    /**
     * Updates the role of a customer.
     * This method performs several checks to ensure that the role update is valid.
     *
     * @param newRole the new role to assign to the customer
     * @throws IllegalArgumentException if the customer or role is not found, or if the customer already has the new role
     * @throws SecurityException        if the update is not permitted due to business rules
     */
    @Transactional
    public void updateCustomerRole(String newRole, Customer customer, Role role) {
        // Check if the new role is different from the existing one
        if (!customer.getRole().getName().equals(newRole)) {
            // Update the customer's role and save the changes
            customer.setRole(role);
            customer.setUpdatedAt(LocalDateTime.now());
            customer.setRoleName(newRole);
            customerRepository.save(customer);

            // Send email notification
            Map<String, Object> templateData = buildTemplateData(customer, newRole);
            String template = "customer_status_update.ftl";
            emailService.sendEmail(template, customer.getEmail(), "Your Role Has Been Updated", templateData);
        }
    }


    /**
     * Retrieves the role of the given user.
     *
     * @param userDetails The user to retrieve the role from.
     * @return The role of the user.
     * @throws IllegalArgumentException If the user has no assigned role.
     */
    public static String getUserRole(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User has no assigned role."));
    }

    /**
     * Updates a customer with the provided details.
     * This method updates only changed fields and retains the old values for unchanged fields.
     *
     * @param id          The ID of the customer to be updated.
     * @param customerDTO The details of the customer to be updated.
     * @return The updated customer.
     */
    public CustomerDTO updateCustomer(String id, CustomerDTO customerDTO) {
        log.debug("Entering updateCustomer with customerId: {}, updated customer: {}", id, customerDTO);

        Customer existingCustomer = customerRepository.findByIdIgnoreCase(id).orElse(null);

        if (existingCustomer == null) {
            log.warn("Customer with ID: {} not found", id);
            return null;
        }

        log.info("Updating customer with ID: {}", id);
        boolean isUpdated = false;


        if (isFieldUpdated(customerDTO.getPhone(), existingCustomer.getPhone())) {
            existingCustomer.setPhone(customerDTO.getPhone());
            log.debug("Updated phone for customer ID: {}", id);
            isUpdated = true;
        }

        if (isFieldUpdated(customerDTO.getStatus(), existingCustomer.getStatus())) {
            existingCustomer.setStatus(customerDTO.getStatus());
            log.debug("Updated status for customer ID: {}", id);
            isUpdated = true;
        }

        if (isUpdated) {
            existingCustomer.setUpdatedAt(LocalDateTime.now());
            customerRepository.save(existingCustomer);
            log.info("Saved updated customer with ID: {}", id);
        } else {
            log.info("No changes detected for customer ID: {}", id);
        }

        return customerMapper.convertToDTO(existingCustomer);
    }

    /**
     * Checks if a field value has been updated.
     *
     * @param newValue the new value of the field
     * @param oldValue the old value of the field
     * @return true if the field has been updated, false otherwise
     */
    private boolean isFieldUpdated(String newValue, String oldValue) {
        return newValue != null && !newValue.equals(oldValue);
    }


    /**
     * Builds a map of data for the email template.
     *
     * @param customer The customer whose data is to be used in the template.
     * @param newRole  The new role of the customer, if applicable.
     * @return A map of data for the email template.
     */
    private Map<String, Object> buildTemplateData(Customer customer, String newRole) {
        Map<String, Object> templateData = new HashMap<>();

        templateData.put("customerEmail", customer.getEmail());
        templateData.put("customerName", customer.getFirstName() + " " + customer.getLastName());
        templateData.put("status", customer.getStatus());
        templateData.put("customerId", customer.getId());

        // Dates
        // Convert LocalDateTime to Date
        templateData.put("creationDate", formatDateTime(customer.getCreatedAt()));
        templateData.put("updatedDate", formatDateTime(customer.getUpdatedAt()));

        // Role information
        String role = newRole != null ? newRole : "Guest User";
        templateData.put("role", role);
        templateData.put("role_type", customer.getRole().getRoleType() != null ? customer.getRole().getRoleType() : "Guest_User");

        // The message to be sent in the email
        String message = "Your request has been " + customer.getStatus() + ". Thank you for your patience and cooperation!";
        templateData.put("message", message);

        return templateData;
    }

    /**
     * Converts a LocalDateTime to a formatted string for display in the email template.
     *
     * @param dateTime The LocalDateTime to format.
     * @return A formatted string representing the given LocalDateTime.
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Not Available";
        }

        // Convert LocalDateTime to Date, so that SimpleDateFormat can be used
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());

        // Format the date as "dd EEEE yyyy hh:mm a"
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd EEEE yyyy hh:mm a");
        return dateFormat.format(date);
    }


    public Optional<CustomerDTO> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email).map(customerMapper::convertToDTO);
    }

    public boolean deleteCustomer(String customerId) {
        Optional<Customer> customer = customerRepository.findByIdIgnoreCase(customerId);
        if (customer.isPresent()) {
            customerRepository.delete(customer.get());
            return true;
        }
        return false;
    }

    public List<CustomerDTO> getCustomersByRole(String roleName) {
        List<Customer> customers = customerRepository.findByRoleNameIgnoreCase(roleName.toUpperCase());
        return customers.stream()
                .map(customerMapper::convertToDTO)
                .toList();
    }

   // public ClientResponse createCustomerWithClient(RegistrationRequest request, Role role, ClientDTO clientDTO) {
//        // Register customer and get the response
//        RegistrationResponse customerResponse = createClient(request, role);
//        log.info("Client registered successfully with ID: {}", customerResponse.getId());
//
//        // Set customer details in ClientDTO and create client
//        clientDTO.setStatus(StatusEnum.Accepted.name());
//        clientDTO.setCustomerId(customerResponse.getId());
//
//        // Create the client and store the response
//        ClientDTO clientResponse = clientService.createClient(customerResponse.getId(), clientDTO);
//
//        // Return a ClientResponse containing both customer and client details
//        return new ClientResponse(
//                customerResponse.getId(),
//                customerResponse.getCustomerNo(),
//                customerResponse.getFirstName(),
//                customerResponse.getLastName(),
//                customerResponse.getEmail(),
//                customerResponse.getPhone(),
//                customerResponse.getStatus(),
//                customerResponse.getRoleName(),
//                clientResponse // Include client details
//        );
//    }


}
