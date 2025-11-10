package com.pradeepit.pit_client_service.dto.response;


import com.pradeepit.pit_client_service.dto.ClientDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public  class ClientResponse extends  RegistrationResponse{
    private ClientDTO clientDTO;

    // Custom Constructor to include RegistrationResponse fields and ClientDTO
    public ClientResponse(String id, String customerNo, String firstName, String lastName, String email,
                          String phone, String status, String roleName, ClientDTO clientDTO) {
        super(id, customerNo, firstName, lastName, email, phone, status, roleName);
        this.clientDTO = clientDTO;
    }
}
