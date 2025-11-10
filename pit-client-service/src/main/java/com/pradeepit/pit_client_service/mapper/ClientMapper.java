package com.pradeepit.pit_client_service.mapper;



import com.pradeepit.pit_client_service.dto.ClientDTO;
import com.pradeepit.pit_client_service.model.Clients;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientMapper INSTANCE = Mappers.getMapper(ClientMapper.class);

    ClientDTO mapToDto(Clients client);

    Clients mapToEntity(ClientDTO clientDTO);

    List<ClientDTO> mapToDtoList(List<Clients> clients);

}
