package top.ajasta.AjastaApp.reservation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BookRequest {
    @NotBlank(message = "date is required")
    // yyyy-MM-dd
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "date must be yyyy-MM-dd")
    private String date;

    @NotBlank(message = "startTime is required")
    // HH:mm 24h
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "startTime must be HH:mm")
    private String startTime;

    @NotBlank(message = "endTime is required")
    // HH:mm 24h
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "endTime must be HH:mm")
    private String endTime;

    @Positive(message = "unit must be positive")
    private Integer unit = 1;
}
