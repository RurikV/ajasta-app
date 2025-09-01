package top.ajasta.AjastaApp.reservation.controller;

import top.ajasta.AjastaApp.reservation.dtos.ResourceDTO;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import top.ajasta.AjastaApp.reservation.services.ResourceService;
import top.ajasta.AjastaApp.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<ResourceDTO>> create(@ModelAttribute @Valid ResourceDTO dto,
                                                        @RequestPart(value = "imageFile", required = true) MultipartFile imageFile) {
        dto.setImageFile(imageFile);
        return ResponseEntity.ok(resourceService.createResource(dto));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<ResourceDTO>> update(@ModelAttribute ResourceDTO dto,
                                                        @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        dto.setImageFile(imageFile);
        return ResponseEntity.ok(resourceService.updateResource(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<?>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.deleteResource(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<ResourceDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @GetMapping
    public ResponseEntity<Response<List<ResourceDTO>>> list(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(resourceService.getResources(type, search, active));
    }
}
