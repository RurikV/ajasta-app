package top.ajasta.AjastaApp.reservation.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceDTO {
    private Long id;

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "type is required")
    private ResourceType type;

    private String location;
    private String description;
    private String imageUrl;
    private MultipartFile imageFile;
    private Boolean active; // use Boolean to allow partial updates
}
