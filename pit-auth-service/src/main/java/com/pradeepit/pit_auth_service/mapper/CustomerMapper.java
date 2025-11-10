package com.pradeepit.pit_auth_service.mapper;
import com.pradeepit.pit_auth_service.dto.CustomerDTO;
import com.pradeepit.pit_auth_service.dto.request.CreateUserRequest;
import com.pradeepit.pit_auth_service.dto.request.RegistrationRequest;
import com.pradeepit.pit_auth_service.dto.response.RegistrationResponse;
import com.pradeepit.pit_auth_service.model.Customer;
import com.pradeepit.pit_auth_service.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    /**
     * Maps a RegistrationRequest to a Customer entity.
     */
    Customer toEntity(RegistrationRequest registrationRequest);

    /**
     * Maps a Customer entity to a RegistrationResponse.
     */
    RegistrationResponse toResponse(Customer customer);

    /**
     * Maps a Customer entity to a CustomerDTO.
     */
    CustomerDTO convertToDTO(Customer customer);

    @Mapping(source = "role", target = "role", qualifiedByName = "mapRole")
    Customer toUserEntity(CreateUserRequest request);


    Customer toClientEntity(RegistrationRequest request);

    @Named("mapRole")
    default Role mapRole(String roleName) {
        if (roleName == null) {
            return null;
        }
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

}