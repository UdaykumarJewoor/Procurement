package com.pradeepit.pit_auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pradeepit.pit_auth_service.dto.CustomerDTO;
import com.pradeepit.pit_auth_service.dto.request.CreateUserRequest;
import com.pradeepit.pit_auth_service.dto.response.RegistrationResponse;
import com.pradeepit.pit_auth_service.model.Customer;
import com.pradeepit.pit_auth_service.model.Role;
import com.pradeepit.pit_auth_service.model.enums.StatusEnum;
import com.pradeepit.pit_auth_service.repository.CustomerRepository;
import com.pradeepit.pit_auth_service.repository.RoleRepository;
import com.pradeepit.pit_auth_service.service.CustomerService;
import com.pradeepit.pit_auth_service.util.CustomerUserDetailsAdapter;
import com.pradeepit.pit_auth_service.util.IdGenerationService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.pradeepit.pit_auth_service.service.CustomerService.getUserRole;


@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class); // Create logger instance

    @Autowired
    private CustomerService customerService;
//    @Autowired
//    private FileStorageService fileStorageService;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private IdGenerationService idGenerationService;
//    @Autowired
//    private ClientRepository clientRepository;


    @Value("${roles.MASTER-ADMIN}")
    private String masterAdminRole;
    @Value("${roles.ADMIN}")
    private String adminRole;
    @Value("${roles.GUEST-USER}")
    private String guestRole;
    @Value("${roles.CLIENT}")
    private String clientRole;


    /**
     * Retrieves all customers.
     *
     * @return List of all customer DTOs
     */
    @GetMapping("/read")
    public ResponseEntity<?> getAllCustomers() {
        List<CustomerDTO> customers = customerService.getAllCustomers();
        if (customers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No customers found"));
        }
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/read/customerId/{id}")
    public boolean isCustomerPresent(@PathVariable String id) {
        return customerService.isCustomerPresent(id);
    }


    /**
     * Retrieves a specific customer by ID.
     *
     * @param customerId The ID of the customer to retrieve
     * @return Customer DTO if found, otherwise a 404 Not Found response
     */
    @GetMapping("/read/{customerId}")
    public ResponseEntity<?> getCustomer(@PathVariable String customerId) {
        Optional<CustomerDTO> customer = customerService.getCustomerById(customerId);
        if (customer.isPresent()) {
            return ResponseEntity.ok(customer.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Customer with id " + customerId + " not found"));
    }

    /**
     * Deletes a customer by ID.
     *
     * @param customerId The ID of the customer to delete
     * @return Response entity with status and message
     */
    @DeleteMapping("/delete/{customerId}")
    public ResponseEntity<?> deleteCustomer(@PathVariable String customerId) {
        Optional<Customer> customer = customerRepository.findByIdIgnoreCase(customerId);

        if (customer.isPresent()) {
            Customer foundCustomer = customer.get();

            // Check if the customer is a masterAdmin and prevent deletion
            if (masterAdminRole.equalsIgnoreCase(foundCustomer.getRole().getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "MasterAdmin cannot be deleted."));
            }

            customerRepository.delete(foundCustomer);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "Customer with id " + customerId + " deleted successfully"));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Customer with id " + customerId + " not found"));
    }


    /**
     * Update the customer status and/or role.
     * This method handles the logic for updating the customer's status and role.
     *
     * @param customerId the ID of the customer whose status and/or role are to be updated
     * @param newStatus  the new status to assign to the customer (can be null)
     * @param newRole    the new role to assign to the customer (can be null)
     * @return ResponseEntity indicating the success or failure of the operation
     */
    @PutMapping("/update/{customerId}/Rule")
    public ResponseEntity<?> updateCustomerRule(@PathVariable String customerId,
                                                @RequestParam(required = false) String newRole,
                                                @RequestParam(required = false) StatusEnum newStatus
    ) {
        UserDetails currentUser = getCurrentUser();  // Assuming this method retrieves the current user
        Optional<Customer> customerOptional = customerRepository.findByIdIgnoreCase(customerId);

        if (customerOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer with ID " + customerId + " not found");
        }

        Customer customer = customerOptional.get();

        // Check if the customer is already declined and the new status is also declined
        if (customer.getStatus().equalsIgnoreCase(StatusEnum.Declined.name()) &&
                newStatus == StatusEnum.Declined) {
            return ResponseEntity.badRequest().body("Customer is already declined.");
        }

        // Validate the status value
        if (newStatus != null && !StatusEnum.Accepted.toString().equalsIgnoreCase(newStatus.name()) &&
                !StatusEnum.Declined.toString().equalsIgnoreCase(newStatus.name())) {
            return ResponseEntity.badRequest().body("Invalid status. Only 'Accepted' or 'Declined' are allowed.");
        }


        // Retrieve roles for current user and target customer
        String currentUserRole = getUserRole(currentUser);
        String targetCustomerRole = (customer.getRole() != null) ? customer.getRole().getName() : guestRole;

        // Prevent admins from updating Master Admin status
        if (adminRole.equalsIgnoreCase(currentUserRole) && masterAdminRole.equalsIgnoreCase(targetCustomerRole)) {
            log.info("{} role cannot update status of {} role", adminRole, masterAdminRole);
            return ResponseEntity.badRequest().body("'" + adminRole + "' role cannot update the status of '" + masterAdminRole + "' role.");
        }

        if (isUpdatingSelf(currentUser, customerId)) {
            return ResponseEntity.badRequest().body("You cannot update your own details.");
        }

        // Admin role cannot assign Master Admin role
        if (adminRole.equalsIgnoreCase(currentUserRole) && masterAdminRole.equalsIgnoreCase(newRole)) {
            log.info("{} role cannot assign {} role", adminRole, masterAdminRole);
            return ResponseEntity.badRequest().body("'" + adminRole + "' role cannot assign '" + masterAdminRole + "' role.");
        }

        // Prevent role update if the new role is the same as the current role
        if (targetCustomerRole.equalsIgnoreCase(newRole)) {
            return ResponseEntity.badRequest().body("Customer already has the role: " + newRole);
        }

        // Prevent users from updating their own role
        if (isUpdatingSelf(currentUser, customerId)) {
            return ResponseEntity.badRequest().body("You cannot update your own role.");
        }

        if (customer.getStatus().equalsIgnoreCase(StatusEnum.Declined.name())) {
            return ResponseEntity.badRequest().body("Cannot update role for a customer with Declined status.");
        }

        // If a new role is provided, fetch the role from the repository
        Optional<Role> role = newRole != null ? roleRepository.findByNameIgnoreCase(newRole) : Optional.empty();

        if (role.isEmpty() && newRole != null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Role name '" + newRole + "' not found.");
        }

        // Call the service to update both status and role if necessary
        try {
            assert newStatus != null;
            customerService.updateCustomerRule(customerId, newStatus, newRole, currentUser, customer, role.orElse(null));

            // Return success response
            return ResponseEntity.ok("Customer details updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating customer details: " + e.getMessage());
        }
    }

    /**
     * Updates the status of a customer.
     *
     * @param customerId The ID of the customer to update
     * @param newStatus  The new status to assign to the customer
     * @return Response entity with the update result
     */
    @PutMapping("/update/{customerId}/client-status")
    public ResponseEntity<?> updateCustomerStatus(@PathVariable String customerId, @RequestParam String newStatus) {
        UserDetails currentUser = getCurrentUser();

        Optional<Customer> customerOptional = customerRepository.findByIdIgnoreCase(customerId);
        if (customerOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer with ID " + customerId + " not found");
        }
        Customer customer = customerOptional.get();

        // Restriction: Current user cannot update their own status
        if (isUpdatingSelf(currentUser, customerId)) {
            return ResponseEntity.badRequest().body("You cannot update your own status.");
        }

        // Validate the status value
        if (!StatusEnum.Accepted.toString().equalsIgnoreCase(newStatus) &&
                !StatusEnum.Declined.toString().equalsIgnoreCase(newStatus)) {
            return ResponseEntity.badRequest().body("Invalid status. Only 'Accepted' or 'Declined' are allowed.");
        }

        // Retrieve roles for current user and target customer
        String currentUserRole = getUserRole(currentUser);
        String targetCustomerRole = customer.getRole().getName();

        if (adminRole.equalsIgnoreCase(currentUserRole) && masterAdminRole.equalsIgnoreCase(targetCustomerRole)) {
            log.info("{} role cannot update status {} role", adminRole, masterAdminRole);
            return ResponseEntity.badRequest().body("'" + adminRole + "' roles cannot update the status of '" + masterAdminRole + "' roles.");
        }

        // Determine status to store in the database
        StatusEnum statusToStore = StatusEnum.valueOf(newStatus);

        log.info("Updating status for customer [{}] to [{}] (Stored as [{}]) by user [{}]",
                customerId, newStatus, statusToStore, currentUser.getUsername());

        // Delegate to the service for the update

        customerService.updateCustomerStatus(customerId, statusToStore, currentUser, customer);
        return ResponseEntity.ok("Customer status updated successfully.");

    }

    /**
     * Updates the role of a customer.
     *
     * @param customerId The ID of the customer whose role will be updated
     * @param newRole    The new role to assign to the customer
     * @return Response entity with the update result
     */
    @PatchMapping("/update/{customerId}/role")
    public ResponseEntity<?> updateCustomerRole(@PathVariable String customerId, @RequestParam String newRole) {
        UserDetails currentUser = getCurrentUser();
        Optional<Customer> customerOptional = customerRepository.findByIdIgnoreCase(customerId);

        if (customerOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer with ID " + customerId + " not found");
        }

        Customer customer = customerOptional.get();
        String currentUserRole = getUserRole(currentUser);
        String targetCustomerRole = customer.getRole().getName();

        // Admin role cannot assign Master Admin role
        if (adminRole.equalsIgnoreCase(currentUserRole) && masterAdminRole.equalsIgnoreCase(newRole)) {
            log.info("{} role cannot assign MASTER-ADMIN role", adminRole);
            return ResponseEntity.badRequest().body("'" + adminRole + "' roles cannot assign 'MASTER-ADMIN' roles.");
        }

        // Prevent updating if the new role is the same as the current role
        if (targetCustomerRole.equalsIgnoreCase(newRole)) {
            return ResponseEntity.badRequest().body("Customer already has the role: " + newRole);
        }

        // Restriction: Admin cannot update Master Admin role
        if (adminRole.equalsIgnoreCase(currentUserRole) && masterAdminRole.equalsIgnoreCase(targetCustomerRole)) {
            log.info("{} role cannot update {} role", adminRole, masterAdminRole);
            return ResponseEntity.badRequest().body("'" + adminRole + "' roles cannot update the role of '" + masterAdminRole + "' roles.");
        }

        // Restriction: UserInfo cannot update their own role
        if (isUpdatingSelf(currentUser, customerId)) {
            return ResponseEntity.badRequest().body("You cannot update your own role.");
        }

        // Restriction: Prevent role updates for Pending or Declined status
        if (customer.getStatus().equalsIgnoreCase(StatusEnum.Pending.name())) {
            return ResponseEntity.badRequest().body("Cannot update role for a customer with Pending status.");
        } else if (customer.getStatus().equalsIgnoreCase(StatusEnum.Declined.name())) {
            return ResponseEntity.badRequest().body("Cannot update role for a customer with Declined status.");
        }


        // Fetch current status from repository
        StatusEnum currentStatus = customerRepository.findStatusByCustomerId(customerId);

        if (currentStatus == StatusEnum.Accepted) {
            return ResponseEntity.badRequest().body("Customer status must be 'Accepted' to update role.");
        }

        Optional<Role> role = roleRepository.findByNameIgnoreCase(newRole);
        if (role.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Role name '" + newRole + "' not found.");
        }

        log.info("Updating role for customer [{}] to [{}] by user [{}]", customerId, newRole, currentUser.getUsername());

        // Update role and status
        customerService.updateCustomerRole(newRole.toUpperCase(), customer, role.get());

        return ResponseEntity.ok("Customer role updated successfully.");
    }

    /**
     * Creates a new user based on the provided request and role.
     *
     * @param request The user creation request data
     * @param role    The role to assign to the user
     * @return Response entity with the registration response
     */
    @PostMapping("/create/user")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request, @RequestParam("role") String role) {
        Optional<Role> roleOptional = roleRepository.findByNameIgnoreCase(role);

        if (roleOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Role not found! Please seed roles first: " + role));
        }

        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email " + request.getEmail() + " is already registered!"));
        }

        RegistrationResponse response = customerService.createUser(request, roleOptional.get());

        if (response == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "User creation failed"));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("data", response, "message", "User created successfully"));
    }


    /**
     * Creates multiple users in a batch.
     *
     * @param requests List of user creation requests
     * @param role     The role to assign to each user
     * @return Response entity with the list of registration responses
     */
    @PostMapping("/create/users")
    public ResponseEntity<?> createUsers(@RequestBody List<CreateUserRequest> requests, @RequestParam("role") String role) {
        Optional<Role> roleOptional = roleRepository.findByNameIgnoreCase(role);

        if (roleOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Role not found! Please seed roles first: " + role));
        }

        List<RegistrationResponse> responses = customerService.createUsers(requests, roleOptional.get());

        if (responses == null || responses.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "User creation failed for the provided requests"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", responses, "message", "UserInfo created successfully"));
    }


    /**
     * Updates customer details by customer ID.
     *
     * @param customerId   The ID of the customer to update
    // * @param image        The new image for the customer (optional)
     * @param customerJson The customer data in JSON format
     * @return Response entity with updated customer data or error message
     */
    @ApiResponses(
            @ApiResponse(responseCode = "200", description = "Customer updated successfully", content = @Content(schema = @Schema(implementation = CustomerDTO.class)))
    )
    @PutMapping(path = "/update/{customerId}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateCustomer(
            @PathVariable String customerId,
//            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart("customer") String customerJson) {
        try {
            CustomerDTO customer = objectMapper.readValue(customerJson, CustomerDTO.class);
            Optional<Customer> customerOptional = customerRepository.findByIdIgnoreCase(customerId);

            if (customerOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer with ID " + customerId + " not found");
            }

//            // Prepare directories for file storage
//            fileStorageService.prepareDirectories();
//
//            // Handle image update
//            if (image != null && !image.isEmpty()) {
//                log.info("Updating customer image for ID: {}", customerId);
//                String updatedImage = processFileUpdate(image, customer.getImage(), customerId + "_Updated");
//                customer.setImage(updatedImage);
//            }

            // Update customer details
            CustomerDTO updatedCustomer = customerService.updateCustomer(customerId, customer);
            return ResponseEntity.ok(updatedCustomer);
        } catch (IOException e) {
            log.error("Error occurred while parsing customer JSON or processing image for ID {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error occurred while updating the customer. Please try again later."));
        } catch (IllegalArgumentException e) {
            log.warn("Customer with ID {} not found during update attempt.", customerId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error occurred while updating customer with ID {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred. Please try again later."));
        }
    }

    /**
     * Utility method to handle file updates.
     *
     * @param newFile     The new file to update (if provided)
     * @param oldFilePath The path of the existing file
     * @param newFileName The new file name to be used if updating
     * @return The updated file path, or the old file path if no update is needed
     * @throws IOException If file operations fail
     */
//    private String processFileUpdate(MultipartFile newFile, String oldFilePath, String newFileName) throws IOException {
//        if (newFile != null && !newFile.isEmpty()) {
//            log.info("Processing {} update for file type: {}", newFileName, "customer-image");
//            log.debug("Saving new file for type: {}, name: {}", "customer-image", newFileName);
//            String directory = fileStorageService.getDirectoryForFileType("customer-image");
//            return fileStorageService.saveFile(newFile, directory, newFileName);
//        }
//        log.debug("No new file provided for type: {}. Retaining old file path.", "customer-image");
//        return oldFilePath;
//    }

    /**
     * Retrieves the currently authenticated user details.
     *
     * @return The current user's details
     */
    @GetMapping("/read/me")
    public ResponseEntity<?> getLoggedInUser() {
        try {
            UserDetails currentUser = getCurrentUser();
            log.info("Current User: {}", currentUser.getUsername());
            Optional<CustomerDTO> customer = customerService.getCustomerByEmail(currentUser.getUsername());
            return customer
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching logged-in user details", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not authenticated"));
        }
    }

    /**
     * Helper method to retrieve the current authenticated user from the security context.
     *
     * @return The current authenticated user
     */
    public static UserDetails getCurrentUser() {
        return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Helper method to check if the current user is trying to update their own role.
     *
     * @param currentUser The current authenticated user
     * @param customerId  The customer ID being updated
     * @return true if the user is trying to update their own role, false otherwise
     */
    private boolean isUpdatingSelf(UserDetails currentUser, String customerId) {
        if (currentUser instanceof CustomerUserDetailsAdapter customerUserDetailsAdapter) {
            String loggedInUserId = customerUserDetailsAdapter.getId();
            return loggedInUserId.equals(customerId);
        }
        return false;
    }

    /**
     * Retrieves the customer ID of the current authenticated user.
     *
     * @return The customer ID of the current authenticated user
     */
    public static String getCustomerId() {
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
     * Get all customers by their role name.
     *
     * @param roleName the role name to search customers by
     * @return list of matching customers
     */
    @GetMapping("read/customers/by-role")
    public ResponseEntity<?> getCustomersByRole(@RequestParam String roleName) {
        List<CustomerDTO> customers = customerService.getCustomersByRole(roleName);

        if (customers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No customers found with role: " + roleName);
        }

        return ResponseEntity.ok(customers);
    }




//    @PostMapping(value = "/create/client", consumes = {"multipart/form-data"})
//    public ResponseEntity<?> registerCustomerWithClient(
//            @RequestPart(value = "companyLogo", required = false) MultipartFile companyLogo,
//            @RequestPart(value = "gstFile", required = false) MultipartFile gstFile,
//            @RequestPart(value = "tanFile", required = false) MultipartFile tanFile,
//            @RequestPart String requestJson) {
//
//        try {
//            //  Parse JSON string into ClientRequest
//            ClientRequest clientRequest = objectMapper.readValue(requestJson, ClientRequest.class);
//
//            ClientDTO clientDTO = clientRequest.getClientDTO();
//            // Validate Role
//            Optional<Role> roleOptional = roleRepository.findByNameIgnoreCase(clientRole);
//            if (roleOptional.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(Map.of("message", "Role with " + clientRole + " not found, please check system configuration."));
//            }
//            Role role = roleOptional.get();
//
//            if (customerRepository.findByEmail(clientRequest.getEmail()).isPresent()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(Map.of("message", "Email " + clientRequest.getEmail() + " is already registered!"));
//            }
//
//            // Check if client already exists
//            Optional<Clients> existingClient = clientRepository.findByCompanyIgnoreCase(clientDTO.getCompany());
//            if (existingClient.isPresent()) {
//                log.warn("Client with name '{}' already exists.", clientDTO.getCompany());
//                return ResponseEntity.status(HttpStatus.CONFLICT)
//                        .body(Map.of("message", "Client with name " + existingClient.get().getCompany() + " already exists."));
//            }
//
//            // Generate Client ID
//            String clientId = idGenerationService.generateClientId();
//            clientDTO.setId(clientId);
//
//
//            // Process file uploads
//            fileStorageService.prepareDirectories();
//            ClientDTO.FileUploadDTO fileUploadDTO = new ClientDTO.FileUploadDTO();
//
//            if (companyLogo != null && !companyLogo.isEmpty()) {
//                String logoDirectory = fileStorageService.getDirectoryForFileType("client-logo");
//                String logoFileName = fileStorageService.saveFile(companyLogo, logoDirectory, clientId);
//                fileUploadDTO.setCompanyLogo(logoFileName);
//            }
//            if (gstFile != null && !gstFile.isEmpty()) {
//                String gstDirectory = fileStorageService.getDirectoryForFileType("client-gst");
//                String gstFileName = fileStorageService.saveFile(gstFile, gstDirectory, clientId);
//                fileUploadDTO.setGstFile(gstFileName);
//            }
//            if (tanFile != null && !tanFile.isEmpty()) {
//                String tanDirectory = fileStorageService.getDirectoryForFileType("client-tan");
//                String tanFileName = fileStorageService.saveFile(tanFile, tanDirectory, clientId);
//                fileUploadDTO.setTanFile(tanFileName);
//            }
//
//            // Attach files to ClientDTO
//            clientDTO.setFileUploads(fileUploadDTO);
//
//            // Register Customer
//            RegistrationResponse response = customerService.createCustomerWithClient(clientRequest, role, clientDTO);
//            if (response != null) {
//                log.info("Successfully added client with ID: {} to customer ID: {}", clientId, response.getId());
//                return ResponseEntity.status(HttpStatus.CREATED)
//                        .body(Map.of(
//                                "message", "Registration Successfully Completed",
//                                "data", response
//                        ));
//            }
//
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(Map.of("message", "Registration Failed"));
//
//        } catch (Exception ex) {
//            log.error("Registration error: {}", ex.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(Map.of("message", ex.getMessage()));
//        }
//    }
}
