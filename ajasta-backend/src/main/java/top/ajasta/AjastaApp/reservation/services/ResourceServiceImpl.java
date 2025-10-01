package top.ajasta.AjastaApp.reservation.services;

import top.ajasta.AjastaApp.aws.AWSS3Service;
import top.ajasta.AjastaApp.exceptions.BadRequestException;
import top.ajasta.AjastaApp.exceptions.NotFoundException;
import top.ajasta.AjastaApp.reservation.dtos.ResourceDTO;
import top.ajasta.AjastaApp.reservation.entity.Resource;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import top.ajasta.AjastaApp.reservation.repository.ResourceRepository;
import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.auth_users.repository.UserRepository;
import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.role.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final AWSS3Service awss3Service;
    private final UserRepository userRepository;

    @Override
    public Response<ResourceDTO> createResource(ResourceDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("Resource name is required");
        }
        if (dto.getType() == null) {
            throw new BadRequestException("Resource type is required");
        }
        MultipartFile imageFile = dto.getImageFile();
        if (imageFile == null || imageFile.isEmpty()) {
            throw new BadRequestException("Resource image is needed");
        }
        String imageUrl;
        String imageName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
        URL s3Url = awss3Service.uploadFile("resources/" + imageName, imageFile);
        imageUrl = s3Url.toString();

        Resource entity = toEntity(dto);
        entity.setId(null);
        entity.setImageUrl(imageUrl);
        // Set managers if provided
        if (dto.getManagerIds() != null) {
            entity.setManagers(resolveManagers(dto.getManagerIds()));
        }
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

        String imageUrl = existing.getImageUrl();
        MultipartFile imageFile = dto.getImageFile();
        if (imageFile != null && !imageFile.isEmpty()) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String keyName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                awss3Service.deleteFile("resources/" + keyName);
            }
            String imageName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            URL newImageUrl = awss3Service.uploadFile("resources/" + imageName, imageFile);
            imageUrl = newImageUrl.toString();
        }

        // update only provided fields
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getType() != null) existing.setType(dto.getType());
        if (dto.getLocation() != null) existing.setLocation(dto.getLocation());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getPricePerSlot() != null) existing.setPricePerSlot(dto.getPricePerSlot());
        if (dto.getActive() != null) existing.setActive(dto.getActive());
        if (dto.getUnitsCount() != null) existing.setUnitsCount(dto.getUnitsCount());
        if (dto.getOpenTime() != null && !dto.getOpenTime().isBlank()) {
            try { existing.setOpenTime(LocalTime.parse(dto.getOpenTime())); } catch (DateTimeParseException ignored) {}
        }
        if (dto.getCloseTime() != null && !dto.getCloseTime().isBlank()) {
            try { existing.setCloseTime(LocalTime.parse(dto.getCloseTime())); } catch (DateTimeParseException ignored) {}
        }
        if (dto.getUnavailableWeekdays() != null) existing.setUnavailableWeekdays(dto.getUnavailableWeekdays());
        if (dto.getUnavailableDates() != null) existing.setUnavailableDates(dto.getUnavailableDates());
        if (dto.getDailyUnavailableRanges() != null) existing.setDailyUnavailableRanges(dto.getDailyUnavailableRanges());
        existing.setImageUrl(imageUrl);

        // Update managers only if provided (allow clearing by sending empty list)
        if (dto.getManagerIds() != null) {
            existing.setManagers(resolveManagers(dto.getManagerIds()));
        }

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
        // delete image from s3 if present
        if (res.getImageUrl() != null && !res.getImageUrl().isEmpty()) {
            String keyName = res.getImageUrl().substring(res.getImageUrl().lastIndexOf("/") + 1);
            awss3Service.deleteFile("resources/" + keyName);
        }
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
                .imageUrl(r.getImageUrl())
                .pricePerSlot(r.getPricePerSlot())
                .active(r.isActive())
                .unitsCount(r.getUnitsCount())
                .openTime(r.getOpenTime() != null ? r.getOpenTime().toString() : null)
                .closeTime(r.getCloseTime() != null ? r.getCloseTime().toString() : null)
                .unavailableWeekdays(r.getUnavailableWeekdays())
                .unavailableDates(r.getUnavailableDates())
                .dailyUnavailableRanges(r.getDailyUnavailableRanges())
                .managerIds(r.getManagers() == null ? null : r.getManagers().stream().map(User::getId).toList())
                .build();
    }

    private Resource toEntity(ResourceDTO dto) {
        Resource.ResourceBuilder b = Resource.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(dto.getType())
                .location(dto.getLocation())
                .description(dto.getDescription())
                .pricePerSlot(dto.getPricePerSlot());
        if (dto.getActive() != null) b.active(dto.getActive());
        if (dto.getUnitsCount() != null) b.unitsCount(dto.getUnitsCount());
        if (dto.getOpenTime() != null && !dto.getOpenTime().isBlank()) {
            try { b.openTime(LocalTime.parse(dto.getOpenTime())); } catch (DateTimeParseException ignored) {}
        }
        if (dto.getCloseTime() != null && !dto.getCloseTime().isBlank()) {
            try { b.closeTime(LocalTime.parse(dto.getCloseTime())); } catch (DateTimeParseException ignored) {}
        }
        if (dto.getUnavailableWeekdays() != null) b.unavailableWeekdays(dto.getUnavailableWeekdays());
        if (dto.getUnavailableDates() != null) b.unavailableDates(dto.getUnavailableDates());
        if (dto.getDailyUnavailableRanges() != null) b.dailyUnavailableRanges(dto.getDailyUnavailableRanges());
        return b.build();
    }

    private List<User> resolveManagers(List<Long> ids) {
        if (ids == null) return null;
        // normalize: unique, non-null
        List<Long> unique = ids.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (unique.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<User> users = userRepository.findAllById(unique);
        java.util.Set<Long> foundIds = users.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
        List<Long> missing = unique.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new NotFoundException("User(s) not found: " + missing);
        }
        // validate role RESOURCE_MANAGER
        List<User> invalid = users.stream()
                .filter(u -> u.getRoles() == null || u.getRoles().stream()
                        .map(Role::getName)
                        .noneMatch(n -> n != null && n.equalsIgnoreCase("RESOURCE_MANAGER")))
                .toList();
        if (!invalid.isEmpty()) {
            String emails = invalid.stream()
                    .map(User::getEmail)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.joining(", "));
            throw new BadRequestException("All assigned managers must have RESOURCE_MANAGER role. Invalid: " + emails);
        }
        return users;
    }
}
