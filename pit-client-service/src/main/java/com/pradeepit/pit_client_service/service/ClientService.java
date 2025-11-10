package com.pradeepit.pit_client_service.service;


import com.pradeepit.pit_client_service.client.CustomerClient;
import com.pradeepit.pit_client_service.dto.ClientDTO;
import com.pradeepit.pit_client_service.dto.SpocDetailsDTO;
import com.pradeepit.pit_client_service.mapper.ClientMapper;
import com.pradeepit.pit_client_service.mapper.SpocDetailsMapper;
import com.pradeepit.pit_client_service.model.Clients;
import com.pradeepit.pit_client_service.model.SpocDetail;
import com.pradeepit.pit_client_service.repository.ClientRepository;
import com.pradeepit.pit_client_service.repository.SpocDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClientService {

    @Autowired
    private SpocDetailsMapper spocDetailsMapper;
    @Autowired
    private SpocDetailsRepository spocDetailsRepository;
    @Autowired
    private ClientRepository clientRepository;
//    @Autowired
//    private IdGenerationService idGenerationService;
    @Autowired
    private ClientMapper clientMapper;
    @Autowired
    private CustomerClient customerClient;

    public String getCustomerId() {
        return customerClient.getCustomerId();
    }

    public boolean isClientPresent(String id) {
        Optional<Clients> clientOptional = clientRepository.findByIdIgnoreCase(id);
        if (clientOptional.isEmpty()) {
            log.info("Client not found with id: {}", id);
            return false;
        }
        return true;
    }

    public ClientDTO createClient(String customerId, ClientDTO clientDTO) {
        log.debug("Entering addClientToCustomer with customerId: {}, clientDTO: {}", customerId, clientDTO);

        Clients client = clientMapper.mapToEntity(clientDTO);
        client.generateClientCode();
        client.setCreatedAt(LocalDateTime.now());
        client.setCreatedBy(customerId);
        client.setLastModifiedBy(null);
        client.setDeleted(false);
        client.setUpdatedAt(null);
        Clients savedClient = clientRepository.save(client);

        // Set default status to "APPROVED"
        //client.setStatus("APPROVED");

        // Set user details (assuming userDetails is coming in clientDTO)
//        if (clientDTO.getUserDetails() != null) {
//            client.setUserDetails(clientDTO.getUserDetails());
//        } else {
//            log.warn("No user details provided for client: {}", client.getCompany());
//        }

        log.info("Saved client entity with clientId: {}", savedClient.getId());

        if (clientDTO.getSpocDetails() != null && !clientDTO.getSpocDetails().isEmpty()) {
            log.info("Adding {} SPOC details for clientId: {}", clientDTO.getSpocDetails().size(), savedClient.getId());

            List<SpocDetail> spocDetailsList = new ArrayList<>();
            for (SpocDetailsDTO spocDTO : clientDTO.getSpocDetails()) {
                SpocDetail spocDetail = spocDetailsMapper.mapToEntity(spocDTO);
                spocDetail.setClientId(savedClient.getId());

//                String spocId = idGenerationService.generateSpocId();
//                spocDetail.setId(spocId);
                spocDetail.setCreatedAt(LocalDateTime.now());
                spocDetail.setCreatedBy(customerId);
                spocDetail.setLastModifiedBy(null);
                spocDetail.setUpdatedAt(null);
                spocDetailsRepository.save(spocDetail);
//                log.debug("Saved SPOC entity with spocId: {} for clientId: {}", spocId, savedClient.getId());

                spocDetailsList.add(spocDetail);
            }

            savedClient.setSpocDetails(spocDetailsList);
        }

        ClientDTO savedClientDTO = clientMapper.mapToDto(savedClient);
        log.info("Exiting addClientToCustomer with saved clientDTO: {}", savedClientDTO);

        return savedClientDTO;
    }

    public List<ClientDTO> getAllClients() {
        List<Clients> clients = clientRepository.findAll();

        // Ensure that SpocDetail objects with null clientId are not included in the grouping
        Map<String, List<SpocDetail>> spocs = spocDetailsRepository.findAll()
                .stream()
                .filter(spoc -> spoc.getClientId() != null)  // Filter out null clientIds
                .collect(Collectors.groupingBy(SpocDetail::getClientId));

        return clients.stream()
                .map(allClients -> {
                    // If no SpocDetails exist for the current client, default to an empty list
                    allClients.setSpocDetails(spocs.getOrDefault(allClients.getId(), new ArrayList<>()));
                    return clientMapper.mapToDto(allClients);
                })
                .collect(Collectors.toList());
    }


    public ClientDTO getClientById(String id) {
        Clients client = clientRepository.findByIdIgnoreCase(id).orElse(null);

        if (client == null) {
            return null;
        }

        List<SpocDetail> spocDetails = spocDetailsRepository.findAll()
                .stream()
                .filter(spoc -> id.equals(spoc.getClientId()))
                .collect(Collectors.toList());

        client.setSpocDetails(spocDetails);

        return clientMapper.mapToDto(client);
    }


//    public void updateClient(String clientId, String customerId, ClientDTO updatedClientDTO) {
//        Optional<Clients> optionalClient = clientRepository.findByIdIgnoreCase(clientId);
//
//        if (optionalClient.isPresent()) {
//            Clients existingClient = optionalClient.get();
//
//            if (updatedClientDTO.getClientCode() != null) {
//                existingClient.setClientCode(updatedClientDTO.getClientCode());
//            }
//            if (updatedClientDTO.getCompany() != null) {
//                existingClient.setCompany(updatedClientDTO.getCompany());
//            }
//            if (updatedClientDTO.getEmail() != null) {
//                existingClient.setEmail(updatedClientDTO.getEmail());
//            }
//            if (updatedClientDTO.getAddressDetails() != null) {
//                existingClient.setAddressDetails(mapToEntityAddress(updatedClientDTO.getAddressDetails()));
//            }
//            if (updatedClientDTO.getContactInfo() != null) {
//                existingClient.setContactInfo(mapToEntityContact(updatedClientDTO.getContactInfo()));
//            }
//            if (updatedClientDTO.getBusinessInfo() != null) {
//                existingClient.setBusinessInfo(mapToEntityBusiness(updatedClientDTO.getBusinessInfo()));
//            }
//            if (updatedClientDTO.getFileUploads() != null) {
//                existingClient.setFileUploads(mapToFileUploadDTO(updatedClientDTO.getFileUploads()));
//            }
//
//            existingClient.setLastModifiedBy(customerId);
//            existingClient.setUpdatedAt(LocalDateTime.now());
//            clientRepository.save(existingClient);
//        }
//    }

    public void updateClient(String clientId, String customerId, ClientDTO updatedClientDTO) {
        Optional<Clients> optionalClient = clientRepository.findByIdIgnoreCase(clientId);

        if (optionalClient.isPresent()) {
            Clients existingClient = optionalClient.get();

            // Update simple fields
            if (updatedClientDTO.getClientCode() != null) {
                existingClient.setClientCode(updatedClientDTO.getClientCode());
            }
            if (updatedClientDTO.getCompany() != null) {
                existingClient.setCompany(updatedClientDTO.getCompany());
            }
            if (updatedClientDTO.getEmail() != null) {
                existingClient.setEmail(updatedClientDTO.getEmail());
            }

            // Update addressDetails only if specific fields are provided
            if (updatedClientDTO.getAddressDetails() != null) {
                if (existingClient.getAddressDetails() == null) {
                    existingClient.setAddressDetails(new Clients.AddressDetails());
                }
                Clients.AddressDetails existingAddress = existingClient.getAddressDetails();
                ClientDTO.AddressDetailsDTO newAddress = updatedClientDTO.getAddressDetails();

                if (newAddress.getCountry() != null) {
                    existingAddress.setCountry(newAddress.getCountry());
                }
                if (newAddress.getState() != null) {
                    existingAddress.setState(newAddress.getState());
                }
                if (newAddress.getCity() != null) {
                    existingAddress.setCity(newAddress.getCity());
                }
                if (newAddress.getZipCode() != null) {
                    existingAddress.setZipCode(newAddress.getZipCode());
                }
                if (newAddress.getStreet() != null) {
                    existingAddress.setStreet(newAddress.getStreet());
                }
            }

            // Update contactInfo only if specific fields are provided
            if (updatedClientDTO.getContactInfo() != null) {
                if (existingClient.getContactInfo() == null) {
                    existingClient.setContactInfo(new Clients.ContactInfo());
                }
                Clients.ContactInfo existingContact = existingClient.getContactInfo();
                ClientDTO.ContactInfoDTO newContact = updatedClientDTO.getContactInfo();

                if (newContact.getWebsite() != null) {
                    existingContact.setWebsite(newContact.getWebsite());
                }
                if (newContact.getPhone() != null) {
                    existingContact.setPhone(newContact.getPhone());
                }
                if (newContact.getTelephone() != null) {
                    existingContact.setTelephone(newContact.getTelephone());
                }
            }

            // Update businessInfo only if specific fields are provided
            if (updatedClientDTO.getBusinessInfo() != null) {
                if (existingClient.getBusinessInfo() == null) {
                    existingClient.setBusinessInfo(new Clients.BusinessInfo());
                }
                Clients.BusinessInfo existingBusiness = existingClient.getBusinessInfo();
                ClientDTO.BusinessInfoDTO newBusiness = updatedClientDTO.getBusinessInfo();

                if (newBusiness.getIndustry() != null) {
                    existingBusiness.setIndustry(newBusiness.getIndustry());
                }
                if (newBusiness.getGstNo() != null) {
                    existingBusiness.setGstNo(newBusiness.getGstNo());
                }
                if (newBusiness.getTanNo() != null) {
                    existingBusiness.setTanNo(newBusiness.getTanNo());
                }
                if (newBusiness.getCurrency() != null) {
                    existingBusiness.setCurrency(newBusiness.getCurrency());
                }
                if (newBusiness.getStatus() != null) {
                    existingBusiness.setStatus(newBusiness.getStatus());
                }
            }

            // Update file uploads only if specific fields are provided
            if (updatedClientDTO.getFileUploads() != null) {
                if (existingClient.getFileUploads() == null) {
                    existingClient.setFileUploads(new Clients.FileUpload());
                }
                Clients.FileUpload existingFileUpload = existingClient.getFileUploads();
                ClientDTO.FileUploadDTO newFiles = updatedClientDTO.getFileUploads();

                if (newFiles.getCompanyLogo() != null) {
                    existingFileUpload.setCompanyLogo(newFiles.getCompanyLogo());
                }
                if (newFiles.getGstFile() != null) {
                    existingFileUpload.setGstFile(newFiles.getGstFile());
                }
                if (newFiles.getTanFile() != null) {
                    existingFileUpload.setTanFile(newFiles.getTanFile());
                }
            }

            existingClient.setLastModifiedBy(customerId);
            existingClient.setUpdatedAt(LocalDateTime.now());
            clientRepository.save(existingClient);
        }
    }


    private Clients.FileUpload mapToFileUploadDTO(ClientDTO.FileUploadDTO fileUploads) {
        return null;
    }

    private Clients.BusinessInfo mapToEntityBusiness(ClientDTO.BusinessInfoDTO businessInfo) {
        return null;
    }

    private Clients.ContactInfo mapToEntityContact(ClientDTO.ContactInfoDTO contactInfo) {
        return null;
    }

    private Clients.AddressDetails mapToEntityAddress(ClientDTO.AddressDetailsDTO addressDetails) {
        return null;
    }


//public ClientDTO updateClient(String clientId, ClientDTO updatedClient, List<MultipartFile> files) {
//    try {
//        logger.info("Updating client with ID: {}", clientId);
//
//        Optional<Clients> existingClientOpt = clientRepository.findById(clientId);
//
//        if (existingClientOpt.isEmpty()) {
//            logger.error("Client with ID {} not found!", clientId);
//            throw new RuntimeException("Client not found with ID: " + clientId);
//        }
//
//        Clients existingClient = existingClientOpt.get();
//
//        // Update only non-null fields from updatedClient
//        if (updatedClient.getCompany() != null) {
//            existingClient.setCompany(updatedClient.getCompany());
//        }
//        if (updatedClient.getEmail() != null) {
//            existingClient.setEmail(updatedClient.getEmail());
//        }
//        if (updatedClient.getAddressDetails() != null) {
//            existingClient.setAddressDetails(mapToEntity(updatedClient.getAddressDetails()));
//        }
//        if (updatedClient.getContactInfo() != null) {
//            existingClient.setContactInfo(updatedClient.getContactInfo());
//        }
//        if (updatedClient.getBusinessInfo() != null) {
//            existingClient.setBusinessInfo(updatedClient.getBusinessInfo());
//        }
//        if (updatedClient.getLastModifiedBy() != null) {
//            existingClient.setLastModifiedBy(updatedClient.getLastModifiedBy());
//        }
//
//        // Handle File Uploads
//        if (files != null && !files.isEmpty()) {
//            List<String> fileNames = new ArrayList<>();
//            for (MultipartFile file : files) {
//                try {
//                    String fileName = file.getOriginalFilename();
//                    fileNames.add(fileName);
//                    logger.info("File {} uploaded successfully for client ID {}", fileName, clientId);
//                } catch (Exception e) {
//                    logger.error("Error uploading file {}: {}", file.getOriginalFilename(), e.getMessage());
//                }
//            }
//            existingClient.setFileUploads(fileNames);
//        }
//
//        ClientDTO savedClient = clientRepository.save(existingClient);
//        logger.info("Client with ID {} updated successfully.", clientId);
//
//        return savedClient;
//    } catch (Exception e) {
//        logger.error("Error updating client with ID {}: {}", clientId, e.getMessage());
//        throw new RuntimeException("Failed to update client: " + e.getMessage());
//    }
//}



    public boolean deleteClient(String id) {
        Optional<Clients> optionalClient = clientRepository.findByIdIgnoreCase(id);
        if (optionalClient.isPresent()) {
            spocDetailsRepository.deleteByClientId(id);
            clientRepository.deleteById(id);
            return true;
        } else {
            log.warn("Client not found with ID: {}", id);
            return false;
        }
    }


}
