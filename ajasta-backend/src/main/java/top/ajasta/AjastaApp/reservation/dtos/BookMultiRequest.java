package top.ajasta.AjastaApp.reservation.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BookMultiRequest {

    @NotNull(message = "days is required")
    @Size(min = 1, message = "days must contain at least one item")
    @Valid
    private List<Day> days;

    @Data
    public static class Day {
        @NotBlank(message = "date is required")
        // yyyy-MM-dd
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "date must be yyyy-MM-dd")
        private String date;

        @NotNull(message = "slots is required")
        @Size(min = 1, message = "slots must contain at least one item")
        @Valid
        private List<BookBatchRequest.Slot> slots;
    }
}
