package top.ajasta.AjastaApp.reservation.services;

import top.ajasta.AjastaApp.exceptions.BadRequestException;
import top.ajasta.AjastaApp.exceptions.NotFoundException;
import top.ajasta.AjastaApp.reservation.dtos.ResourceDTO;
import top.ajasta.AjastaApp.reservation.entity.Resource;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import top.ajasta.AjastaApp.reservation.repository.ResourceRepository;
import top.ajasta.AjastaApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;

    @Override
    public Response<ResourceDTO> createResource(ResourceDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("Resource name is required");
        }
        if (dto.getType() == null) {
            throw new BadRequestException("Resource type is required");
        }
        Resource entity = toEntity(dto);
        entity.setId(null);
        Resource saved = resourceRepository.save(entity);
        return Response.<ResourceDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Resource created successfully")
                .data(toDTO(saved))
                .build();
    }

    @Override
    public Response<ResourceDTO> updateResource(ResourceDTO dto) {
        if (dto.getId() == null) {
            throw new BadRequestException("Resource id is required for update");
        }
        Resource existing = resourceRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        // update only provided fields
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getType() != null) existing.setType(dto.getType());
        if (dto.getLocation() != null) existing.setLocation(dto.getLocation());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getActive() != null) existing.setActive(dto.getActive());
        Resource saved = resourceRepository.save(existing);
        return Response.<ResourceDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Resource updated successfully")
                .data(toDTO(saved))
                .build();
    }

    @Override
    public Response<ResourceDTO> getResourceById(Long id) {
        Resource res = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        return Response.<ResourceDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Resource fetched successfully")
                .data(toDTO(res))
                .build();
    }

    @Override
    public Response<?> deleteResource(Long id) {
        Resource res = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        resourceRepository.delete(res);
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Resource deleted successfully")
                .build();
    }

    @Override
    public Response<List<ResourceDTO>> getResources(ResourceType type, String search, Boolean active) {
        List<Resource> all = resourceRepository.findAll();
        List<ResourceDTO> data = all.stream()
                .filter(r -> type == null || r.getType() == type)
                .filter(r -> active == null || r.isActive() == active)
                .filter(r -> search == null || search.isBlank() ||
                        (r.getName() != null && r.getName().toLowerCase().contains(search.toLowerCase())) ||
                        (r.getLocation() != null && r.getLocation().toLowerCase().contains(search.toLowerCase())))
                .map(this::toDTO)
                .collect(Collectors.toList());
        return Response.<List<ResourceDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Resources fetched successfully")
                .data(data)
                .build();
    }

    private ResourceDTO toDTO(Resource r) {
        return ResourceDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .type(r.getType())
                .location(r.getLocation())
                .description(r.getDescription())
                .active(r.isActive())
                .build();
    }

    private Resource toEntity(ResourceDTO dto) {
        Resource.ResourceBuilder b = Resource.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(dto.getType())
                .location(dto.getLocation())
                .description(dto.getDescription());
        if (dto.getActive() != null) b.active(dto.getActive());
        return b.build();
    }
}
