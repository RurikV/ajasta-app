package top.ajasta.AjastaApp.reservation.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BookBatchRequest {

    @NotBlank(message = "date is required")
    // yyyy-MM-dd
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "date must be yyyy-MM-dd")
    private String date;

    @NotNull(message = "slots is required")
    @Size(min = 1, message = "slots must contain at least one item")
    @Valid
    private List<Slot> slots;

    @Data
    public static class Slot {
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
}
