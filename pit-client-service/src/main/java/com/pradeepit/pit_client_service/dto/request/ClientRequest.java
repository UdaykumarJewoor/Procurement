package com.pradeepit.pit_client_service.dto.request;



import com.pradeepit.pit_client_service.dto.ClientDTO;
import com.pradeepit.pit_client_service.dto.RegistrationRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientRequest extends RegistrationRequest {
    private ClientDTO clientDTO;

}
