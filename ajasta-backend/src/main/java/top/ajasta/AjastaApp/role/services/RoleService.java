package top.ajasta.AjastaApp.role.services;

import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.role.dtos.RoleDTO;

import java.util.List;

public interface RoleService {


    Response<RoleDTO> createRole(RoleDTO roleDTO);

    Response<RoleDTO> updateRole(RoleDTO roleDTO);

    Response<List<RoleDTO>> getAllRoles();

    Response<?> deleteRole(Long id);
}
