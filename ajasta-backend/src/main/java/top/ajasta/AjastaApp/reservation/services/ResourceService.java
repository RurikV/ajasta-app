package top.ajasta.AjastaApp.reservation.services;

import top.ajasta.AjastaApp.reservation.dtos.ResourceDTO;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import top.ajasta.AjastaApp.response.Response;

import java.util.List;

public interface ResourceService {
    Response<ResourceDTO> createResource(ResourceDTO dto);
    Response<ResourceDTO> updateResource(ResourceDTO dto);
    Response<ResourceDTO> getResourceById(Long id);
    Response<?> deleteResource(Long id);
    Response<List<ResourceDTO>> getResources(ResourceType type, String search, Boolean active);
}
