package top.ajasta.AjastaApp.reservation.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

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

    // Price per 30-minute booking slot
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal pricePerSlot;

    // Scheduling fields (strings for easy transport)
    private Integer unitsCount; // number of simultaneous units
    private String openTime; // HH:mm
    private String closeTime; // HH:mm

    // Unavailability config (CSV / semicolon separated)
    private String unavailableWeekdays; // e.g., 0,6
    private String unavailableDates; // yyyy-MM-dd,yyyy-MM-dd
    private String dailyUnavailableRanges; // e.g., 12:00-13:30;16:00-17:00

    // IDs of users who manage this resource
    private List<Long> managerIds;
}
