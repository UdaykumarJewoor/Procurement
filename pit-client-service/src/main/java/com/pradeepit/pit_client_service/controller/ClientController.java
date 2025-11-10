package com.pradeepit.pit_client_service.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pradeepit.pit_client_service.dto.ClientDTO;
import com.pradeepit.pit_client_service.model.Clients;
import com.pradeepit.pit_client_service.repository.ClientRepository;
import com.pradeepit.pit_client_service.service.ClientService;
import com.pradeepit.pit_client_service.util.FileStorageService;
import com.pradeepit.pit_client_service.util.IdGenerationService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;



@RestController
@RequestMapping("/api/clients")
@Slf4j
public class ClientController {
    @Autowired
    private ClientService clientService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private IdGenerationService idGenerationService;
    @Autowired
    private ClientRepository clientRepository;

    @GetMapping("/read/clientId/{id}")
    public boolean isClientPresent(@PathVariable String id) {
        return clientService.isClientPresent(id);
    }

    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
    public ResponseEntity<?> addClientToCustomer(
            @RequestPart(value = "companyLogo", required = false) MultipartFile companyLogo,
            @RequestPart(value = "gstFile", required = false) MultipartFile gstFile,
            @RequestPart(value = "tanFile", required = false) MultipartFile tanFile,
            @RequestPart("client") String clientDTOJson) {
        String customerId = clientService.getCustomerId();


        try {

            ClientDTO clientDTO = objectMapper.readValue(clientDTOJson, ClientDTO.class);

            Optional<Clients> existingClient = clientRepository.findByCompanyIgnoreCase(clientDTO.getCompany());

            if (existingClient.isPresent()) {
                log.warn("Client with name '{}' already exists.", clientDTO.getCompany());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Client with name " + existingClient.get().getCompany() + " already exists."));
            }

            // Prepare directories for file storage
            fileStorageService.prepareDirectories();

            // Generate and set client ID
            String clientId = idGenerationService.generateClientId();
            clientDTO.setId(clientId);

            ClientDTO.FileUploadDTO fileUploadDTO = new ClientDTO.FileUploadDTO();

            // Process the company logo file, if provided
            if (companyLogo != null && !companyLogo.isEmpty()) {
                log.debug("Processing company logo for client ID {}:", clientId);
                String logoDirectory = fileStorageService.getDirectoryForFileType("client-logo");
                String logoFileName = fileStorageService.saveFile(companyLogo, logoDirectory, clientId);
                fileUploadDTO.setCompanyLogo(logoFileName);
                log.info("Company logo saved for client ID {}:", clientId);
            }

            // Process the GST file, if provided
            if (gstFile != null && !gstFile.isEmpty()) {
                log.debug("Processing GST file for client ID {}:", clientId);
                String gstDirectory = fileStorageService.getDirectoryForFileType("client-gst");
                String gstFileName = fileStorageService.saveFile(gstFile, gstDirectory, clientId);
                fileUploadDTO.setGstFile(gstFileName);
                log.info("GST file saved for client ID {}:", clientId);
            }

            // Process the TAN file, if provided
            if (tanFile != null && !tanFile.isEmpty()) {
                log.debug("Processing TAN file for client ID {}:", clientId);
                String tanDirectory = fileStorageService.getDirectoryForFileType("client-tan");
                String tanFileName = fileStorageService.saveFile(tanFile, tanDirectory, clientId);
                fileUploadDTO.setTanFile(tanFileName);
                log.info("TAN file saved for client ID {}:", clientId);
            }

            // Attach file uploads to the client DTO
            clientDTO.setFileUploads(fileUploadDTO);

            // Save the client to the customer
            log.info("Saving new client for customer ID: {}", customerId);
            ClientDTO savedClient = clientService.createClient(customerId, clientDTO);
            log.info("Successfully added client with ID: {} to customer ID: {}", clientId, customerId);

            // Return response with the saved client data
            return ResponseEntity.status(HttpStatus.CREATED).body(savedClient);

        } catch (IOException e) {
            log.error("Error while saving files or adding client for customer ID {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error occurred while saving files or adding client. Please try again later."));
        } catch (Exception e) {
            log.error("Unexpected error while adding client to customer ID {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred. Please try again later."));
        }
    }

    @PostMapping(value = "/create/multiple", consumes = {"multipart/form-data"})
    public ResponseEntity<?> addClientsToCustomer(
            @RequestPart(value = "companyLogos", required = false) List<MultipartFile> companyLogos,
            @RequestPart(value = "gstFiles", required = false) List<MultipartFile> gstFiles,
            @RequestPart(value = "tanFiles", required = false) List<MultipartFile> tanFiles,
            @RequestPart("clients") String clientsDTOJson) {

        String customerId = clientService.getCustomerId();

        try {
            // Convert JSON string to a list of ClientDTO objects
            List<ClientDTO> clientDTOs = objectMapper.readValue(clientsDTOJson, new TypeReference<List<ClientDTO>>() {
            });

            List<ClientDTO> savedClients = new ArrayList<>();
            List<Map<String, String>> failedClients = new ArrayList<>();

            for (int i = 0; i < clientDTOs.size(); i++) {
                ClientDTO clientDTO = clientDTOs.get(i);

                // Check if the client already exists
                Optional<Clients> existingClient = clientRepository.findByCompanyIgnoreCase(clientDTO.getCompany());
                if (existingClient.isPresent()) {
                    log.warn("Client '{}' already exists.", clientDTO.getCompany());
                    failedClients.add(Map.of("company", clientDTO.getCompany(), "message", "Client already exists."));
                    continue; // Skip to the next client
                }

                // Prepare directories for file storage
                fileStorageService.prepareDirectories();

                // Generate and set client ID
                String clientId = idGenerationService.generateClientId();
                clientDTO.setId(clientId);

                ClientDTO.FileUploadDTO fileUploadDTO = new ClientDTO.FileUploadDTO();

                // Process file uploads if provided
                if (companyLogos != null && i < companyLogos.size() && !companyLogos.get(i).isEmpty()) {
                    String logoDirectory = fileStorageService.getDirectoryForFileType("client-logo");
                    String logoFileName = fileStorageService.saveFile(companyLogos.get(i), logoDirectory, clientId);
                    fileUploadDTO.setCompanyLogo(logoFileName);
                }
                if (gstFiles != null && i < gstFiles.size() && !gstFiles.get(i).isEmpty()) {
                    String gstDirectory = fileStorageService.getDirectoryForFileType("client-gst");
                    String gstFileName = fileStorageService.saveFile(gstFiles.get(i), gstDirectory, clientId);
                    fileUploadDTO.setGstFile(gstFileName);
                }
                if (tanFiles != null && i < tanFiles.size() && !tanFiles.get(i).isEmpty()) {
                    String tanDirectory = fileStorageService.getDirectoryForFileType("client-tan");
                    String tanFileName = fileStorageService.saveFile(tanFiles.get(i), tanDirectory, clientId);
                    fileUploadDTO.setTanFile(tanFileName);
                }

                // Attach files and save the client
                clientDTO.setFileUploads(fileUploadDTO);
                ClientDTO savedClient = clientService.createClient(customerId, clientDTO);
                savedClients.add(savedClient);
            }

            // Return response with successful and failed insertions
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", savedClients, "failed", failedClients));

        } catch (IOException e) {
            log.error("Error while processing bulk client creation for customer ID {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error occurred while processing bulk client creation."));
        } catch (Exception e) {
            log.error("Unexpected error in bulk client creation for customer ID {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred."));
        }
    }

    @GetMapping("/read")
    public ResponseEntity<?> getAllClients() {
        List<ClientDTO> clientDTOS = clientService.getAllClients();
        if (clientDTOS.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No clients found."));
        }
        return ResponseEntity.ok(clientDTOS);
    }

    @GetMapping("/read/{clientId}")
    public ResponseEntity<?> getClientById(@PathVariable String clientId) {
        ClientDTO clientDTO = clientService.getClientById(clientId);
        if (clientDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Client with id " + clientId + " not found."));
        }
        return ResponseEntity.ok(clientDTO);
    }

    @DeleteMapping("/delete/{clientId}")
    public ResponseEntity<?> deleteClientById(@PathVariable String clientId) {
        boolean isDeleted = clientService.deleteClient(clientId);
        if (isDeleted) {
            return ResponseEntity.ok(Map.of("message", "Client with id " + clientId + " deleted successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Client with id " + clientId + " not found"));
        }
    }

    private String processFileUpdate(MultipartFile newFile, String oldFilePath, String newFileName) throws IOException {
        if (newFile != null && !newFile.isEmpty()) {
            if (oldFilePath != null) {
                fileStorageService.moveToTrash(oldFilePath, "client-files");
            }
            String directory = fileStorageService.getDirectoryForFileType("client-files");
            return fileStorageService.saveFile(newFile, directory, newFileName);
        }
        return oldFilePath;
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client details updated successfully", content = @Content(schema = @Schema(implementation = ClientDTO.class))),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "500", description = "An error occurred while processing files or updating the client.")
    })
    @PutMapping(value = "/update/{clientId}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateClient(
            @PathVariable String clientId,
            @RequestPart(value = "gstFile", required = false) MultipartFile gstFile,
            @RequestPart(value = "tanFile", required = false) MultipartFile tanFile,
            @RequestPart(value = "companyLogo", required = false) MultipartFile companyLogo,
            @RequestPart("client") String clientJson
    ) {
        String id = clientId.toUpperCase();
        try {
            // Deserialize Client JSON
            ClientDTO updatedClientDTO = objectMapper.readValue(clientJson, ClientDTO.class);

            // Fetch existing Client
            Optional<Clients> client = clientRepository.findByIdIgnoreCase(clientId);
            if (client.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Client with id " + id + " not found."
                ));
            }
            Clients existingClient = client.get();

            // Prepare directories for file uploads
            fileStorageService.prepareDirectories();

            // Get existing file uploads or initialize new ones
            Clients.FileUpload existingFileUpload =
                    existingClient.getFileUploads() != null ? existingClient.getFileUploads() : new Clients.FileUpload();

            // Handle GST File update if provided
            if (gstFile != null && !gstFile.isEmpty()) {
                log.debug("Processing GST File update for client ID: {}", clientId);
                String gstFileName = processFileUpdate(gstFile, existingFileUpload.getGstFile(), id + "_Updated_GST");
                existingFileUpload.setGstFile(gstFileName);
                log.info("GST File updated for client ID: {}", clientId);
            }

            // Handle TAN File update if provided
            if (tanFile != null && !tanFile.isEmpty()) {
                log.debug("Processing TAN File update for client ID: {}", clientId);
                String tanFileName = processFileUpdate(tanFile, existingFileUpload.getTanFile(), id + "_Updated_TAN");
                existingFileUpload.setTanFile(tanFileName);
                log.info("TAN File updated for client ID: {}", clientId);
            }

            // Handle Company Logo update if provided
            if (companyLogo != null && !companyLogo.isEmpty()) {
                log.debug("Processing Company Logo update for client ID: {}", clientId);
                String logoFileName = processFileUpdate(companyLogo, existingFileUpload.getCompanyLogo(), id + "_Updated_Logo");
                existingFileUpload.setCompanyLogo(logoFileName);
                log.info("Company Logo updated for client ID: {}", clientId);
            }

            // Set the updated file uploads to the client DTO
            updatedClientDTO.setFileUploads(
                    new ClientDTO.FileUploadDTO(existingFileUpload.getCompanyLogo(),
                            existingFileUpload.getGstFile(),
                            existingFileUpload.getTanFile())
            );

            String customerId = clientService.getCustomerId();
            clientService.updateClient(clientId, customerId, updatedClientDTO);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "message", "Client with id " + id + " updated successfully"
            ));
        } catch (IOException e) {
            log.error("Error occurred while updating client with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while processing files or updating the client."));
        } catch (IllegalArgumentException e) {
            log.error("Client not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Client with id " + id + " not found"));
        } catch (Exception e) {
            log.error("Unexpected error occurred while updating client: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred. Please try again later."));
        }
    }
}