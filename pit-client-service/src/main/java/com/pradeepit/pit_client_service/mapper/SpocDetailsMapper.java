package com.pradeepit.pit_client_service.mapper;



import com.pradeepit.pit_client_service.dto.SpocDetailsDTO;
import com.pradeepit.pit_client_service.model.SpocDetail;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SpocDetailsMapper {

    SpocDetailsMapper INSTANCE = Mappers.getMapper(SpocDetailsMapper.class);

    SpocDetail mapToEntity(SpocDetailsDTO spocDetailsDTO);

    SpocDetailsDTO mapToDto(SpocDetail spocDetails);
}
