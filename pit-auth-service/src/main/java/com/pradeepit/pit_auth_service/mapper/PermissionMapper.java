package com.pradeepit.pit_auth_service.mapper;


import com.pradeepit.pit_auth_service.dto.*;
import com.pradeepit.pit_auth_service.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionMapper INSTANCE = Mappers.getMapper(PermissionMapper.class);

    Permission dtoToPermission(PermissionDTO permissionDTO);

    PermissionDTO permissionToDTO(Permission permission);

    PermissionHistoryDTO permissionHistoryToDTO(PermissionHistory permissionHistory);
}
