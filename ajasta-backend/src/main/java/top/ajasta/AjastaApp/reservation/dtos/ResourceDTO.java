package top.ajasta.AjastaApp.reservation.dtos;

import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDTO {
    private Long id;

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "type is required")
    private ResourceType type;

    private String location;
    private String description;
    private Boolean active; // use Boolean to allow partial updates
}
