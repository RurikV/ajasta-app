package top.ajasta.AjastaApp.reservation.controller;

import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.auth_users.services.UserService;
import top.ajasta.AjastaApp.email_notification.dtos.NotificationDTO;
import top.ajasta.AjastaApp.email_notification.services.NotificationService;
import top.ajasta.AjastaApp.reservation.dtos.ResourceDTO;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import top.ajasta.AjastaApp.reservation.services.ResourceService;
import top.ajasta.AjastaApp.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final TemplateEngine templateEngine;

    @Value("${base.payment.link}")
    private String basePaymentLink;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<ResourceDTO>> create(@ModelAttribute @Valid ResourceDTO dto,
                                                        @RequestPart(value = "imageFile") MultipartFile imageFile) {
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

    @PostMapping("/{id}/book")
    @PreAuthorize("hasAnyAuthority('CUSTOMER','ADMIN')")
    public ResponseEntity<Response<?>> book(@PathVariable Long id, @RequestBody @Valid top.ajasta.AjastaApp.reservation.dtos.BookRequest request) {
        // Keep single-slot endpoint for compatibility but do not aggregate emails here
        ResourceDTO resource = resourceService.getResourceById(id).getData();
        User user = userService.getCurrentLoggedInUser();

        String subject = "Booking Confirmation - " + (resource != null ? resource.getName() : ("Resource #" + id));

        String totalAmount = resource != null && resource.getPricePerSlot() != null
                ? resource.getPricePerSlot().toString()
                : "0.00";
        // single 30-min slot

        String paymentLink = basePaymentLink + "B" + id + "&amount=" + totalAmount;

        Context context = new Context();
        context.setVariable("customerName", user.getName() != null ? user.getName() : "Customer");
        context.setVariable("resourceName", resource != null ? resource.getName() : ("#" + id));
        context.setVariable("resourceLocation", resource != null ? resource.getLocation() : "");
        context.setVariable("date", safe(request.getDate()));
        context.setVariable("timeRange", safe(request.getStartTime()) + " - " + safe(request.getEndTime()));
        context.setVariable("unit", request.getUnit() != null ? request.getUnit() : 1);
        context.setVariable("pricePerSlot", totalAmount);
        context.setVariable("totalAmount", totalAmount);
        context.setVariable("paymentLink", paymentLink);
        context.setVariable("currentYear", java.time.Year.now());

        String emailBody = templateEngine.process("booking-confirmation", context);

        notificationService.sendEmail(NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject(subject)
                .body(emailBody)
                .isHtml(true)
                .build());

        return ResponseEntity.ok(Response.builder()
                .statusCode(200)
                .message("Booking request accepted")
                .build());
    }

    @PostMapping("/{id}/book-batch")
    @PreAuthorize("hasAnyAuthority('CUSTOMER','ADMIN')")
    public ResponseEntity<Response<?>> bookBatch(@PathVariable Long id, @RequestBody @Valid top.ajasta.AjastaApp.reservation.dtos.BookBatchRequest request) {
        // Fetch resource and user
        ResourceDTO resource = resourceService.getResourceById(id).getData();
        User user = userService.getCurrentLoggedInUser();

        String subject = "Booking Confirmation - " + (resource != null ? resource.getName() : ("Resource #" + id));

        // Determine pricing
        java.math.BigDecimal perSlot = resource != null && resource.getPricePerSlot() != null
                ? resource.getPricePerSlot()
                : java.math.BigDecimal.ZERO;
        int totalSlots = request.getSlots() != null ? request.getSlots().size() : 0;
        java.math.BigDecimal totalAmountBD = perSlot.multiply(java.math.BigDecimal.valueOf(totalSlots));

        String pricePerSlot = perSlot.toString();
        String totalAmount = totalAmountBD.toString();

        // Build HTML list of slots
        StringBuilder slotsHtml = new StringBuilder();
        if (request.getSlots() != null) {
            for (top.ajasta.AjastaApp.reservation.dtos.BookBatchRequest.Slot s : request.getSlots()) {
                slotsHtml.append("<div class=\"row\">")
                        .append("<span>")
                        .append(s.getStartTime()).append(" - ").append(s.getEndTime())
                        .append("</span>")
                        .append("<span> | Unit ").append(s.getUnit() == null ? 1 : s.getUnit()).append("</span>")
                        .append("</div>");
            }
        }

        // Build payment link similar to order confirmations
        String paymentLink = basePaymentLink + "B" + id + "&amount=" + totalAmount;

        // Prepare Thymeleaf context
        Context context = new Context();
        context.setVariable("customerName", user.getName() != null ? user.getName() : "Customer");
        context.setVariable("resourceName", resource != null ? resource.getName() : ("#" + id));
        context.setVariable("resourceLocation", resource != null ? resource.getLocation() : "");
        context.setVariable("date", safe(request.getDate()));
        // Indicate multiple
        context.setVariable("timeRange", "Multiple slots");
        context.setVariable("totalSlots", totalSlots);
        context.setVariable("slotsHtml", slotsHtml.toString());
        context.setVariable("pricePerSlot", pricePerSlot);
        context.setVariable("totalAmount", totalAmount);
        context.setVariable("paymentLink", paymentLink);
        context.setVariable("currentYear", java.time.Year.now());

        String emailBody = templateEngine.process("booking-confirmation", context);

        notificationService.sendEmail(NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject(subject)
                .body(emailBody)
                .isHtml(true)
                .build());

        return ResponseEntity.ok(Response.builder()
                .statusCode(200)
                .message("Booking request accepted for " + totalSlots + " slot(s)")
                .build());
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
