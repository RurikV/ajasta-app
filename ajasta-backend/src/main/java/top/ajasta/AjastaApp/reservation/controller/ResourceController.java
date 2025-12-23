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
import top.ajasta.AjastaApp.exceptions.UnauthorizedAccessException;
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
    private final top.ajasta.AjastaApp.order.services.OrderService orderService;

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
    @PreAuthorize("hasAnyAuthority('ADMIN','RESOURCE_MANAGER')")
    public ResponseEntity<Response<ResourceDTO>> update(@ModelAttribute ResourceDTO dto,
                                                        @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        dto.setImageFile(imageFile);

        // If caller is a RESOURCE_MANAGER (not ADMIN), ensure they manage this resource
        User current = userService.getCurrentLoggedInUser();
        boolean isAdmin = current.getRoles() != null && current.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));
        if (!isAdmin) {
            if (dto.getId() == null) {
                throw new UnauthorizedAccessException("Only admins can create resources");
            }
            Response<ResourceDTO> existing = resourceService.getResourceById(dto.getId());
            List<Long> mgrIds = existing.getData() != null ? existing.getData().getManagerIds() : null;
            if (mgrIds == null || !mgrIds.contains(current.getId())) {
                throw new UnauthorizedAccessException("You are not a manager of this resource");
            }
        }

        return ResponseEntity.ok(resourceService.updateResource(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','RESOURCE_MANAGER')")
    public ResponseEntity<Response<?>> delete(@PathVariable Long id) {
        // If caller is a RESOURCE_MANAGER (not ADMIN), ensure they manage this resource
        User current = userService.getCurrentLoggedInUser();
        boolean isAdmin = current.getRoles() != null && current.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));
        if (!isAdmin) {
            Response<ResourceDTO> existing = resourceService.getResourceById(id);
            List<Long> mgrIds = existing.getData() != null ? existing.getData().getManagerIds() : null;
            if (mgrIds == null || !mgrIds.contains(current.getId())) {
                throw new UnauthorizedAccessException("You are not a manager of this resource");
            }
        }
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

        // Record booking as an order entry in user's history (single slot)
        java.math.BigDecimal perSlot = resource != null && resource.getPricePerSlot() != null
                ? resource.getPricePerSlot()
                : java.math.BigDecimal.ZERO;
        try {
            String bookingTitle = "Booking: " + (resource != null ? resource.getName() : ("Resource #" + id));
            String bookingDetails = new StringBuilder()
                    .append("Date: ").append(safe(request.getDate())).append("\n")
                    .append("Time: ").append(safe(request.getStartTime())).append(" - ").append(safe(request.getEndTime())).append("\n")
                    .append("Unit: ").append(request.getUnit() != null ? request.getUnit() : 1).append("\n")
                    .append("Price per slot: ").append(totalAmount).append("\n")
                    .append("Total: ").append(totalAmount)
                    .toString();
            // Bind resource context so the order is associated to this resource
            orderService.setCurrentBookingResourceId(id);
            orderService.createBookingOrder(perSlot, bookingTitle, bookingDetails);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Response.builder()
                .statusCode(200)
                .message("Your booking has been received. We've sent a secure payment link to your email.")
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

        // Record booking as an order entry in user's history (batch slots)
        try {
            String bookingTitle = "Booking: " + (resource != null ? resource.getName() : ("Resource #" + id)) + " (" + totalSlots + " slot(s))";
            StringBuilder textSlots = new StringBuilder();
            if (request.getSlots() != null) {
                for (top.ajasta.AjastaApp.reservation.dtos.BookBatchRequest.Slot s : request.getSlots()) {
                    textSlots.append("- ")
                            .append(s.getStartTime()).append(" - ").append(s.getEndTime())
                            .append(" | Unit ").append(s.getUnit() == null ? 1 : s.getUnit())
                            .append("\n");
                }
            }
            String bookingDetails = new StringBuilder()
                    .append("Date: ").append(safe(request.getDate())).append("\n")
                    .append("Total slots: ").append(totalSlots).append("\n")
                    .append(textSlots)
                    .append("Price per slot: ").append(pricePerSlot).append("\n")
                    .append("Total: ").append(totalAmount)
                    .toString();
            orderService.setCurrentBookingResourceId(id);
            orderService.createBookingOrder(totalAmountBD, bookingTitle, bookingDetails);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Response.builder()
                .statusCode(200)
                .message("Your booking has been received for " + totalSlots + " slot(s). We've sent a secure payment link to your email.")
                .build());
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    @PostMapping("/{id}/book-multi")
    @PreAuthorize("hasAnyAuthority('CUSTOMER','ADMIN')")
    public ResponseEntity<Response<?>> bookMulti(@PathVariable Long id, @RequestBody @Valid top.ajasta.AjastaApp.reservation.dtos.BookMultiRequest request) {
        // Fetch resource and user
        ResourceDTO resource = resourceService.getResourceById(id).getData();
        User user = userService.getCurrentLoggedInUser();

        String subject = "Booking Confirmation - " + (resource != null ? resource.getName() : ("Resource #" + id));

        // Determine pricing
        java.math.BigDecimal perSlot = resource != null && resource.getPricePerSlot() != null
                ? resource.getPricePerSlot()
                : java.math.BigDecimal.ZERO;

        int totalSlots = 0;
        StringBuilder slotsHtml = new StringBuilder();
        if (request.getDays() != null) {
            for (top.ajasta.AjastaApp.reservation.dtos.BookMultiRequest.Day day : request.getDays()) {
                // Date header
                slotsHtml.append("<div class=\"row\"><span class=\"label\">Date:</span> <span class=\"value\">")
                        .append(safe(day.getDate()))
                        .append("</span></div>");
                if (day.getSlots() != null) {
                    for (top.ajasta.AjastaApp.reservation.dtos.BookBatchRequest.Slot s : day.getSlots()) {
                        totalSlots++;
                        slotsHtml.append("<div class=\"row\">")
                                .append("<span>")
                                .append(s.getStartTime()).append(" - ").append(s.getEndTime())
                                .append("</span>")
                                .append("<span> | Unit ").append(s.getUnit() == null ? 1 : s.getUnit()).append("</span>")
                                .append("</div>");
                    }
                }
            }
        }

        java.math.BigDecimal totalAmountBD = perSlot.multiply(java.math.BigDecimal.valueOf(totalSlots));
        String pricePerSlot = perSlot.toString();
        String totalAmount = totalAmountBD.toString();

        String paymentLink = basePaymentLink + "B" + id + "&amount=" + totalAmount;

        Context context = new Context();
        context.setVariable("customerName", user.getName() != null ? user.getName() : "Customer");
        context.setVariable("resourceName", resource != null ? resource.getName() : ("#" + id));
        context.setVariable("resourceLocation", resource != null ? resource.getLocation() : "");
        context.setVariable("date", "Multiple dates");
        context.setVariable("timeRange", "Multiple days");
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

        // Record booking as an order entry in user's history (multi-day)
        try {
            int totalDays = request.getDays() == null ? 0 : request.getDays().size();
            String bookingTitle = "Booking: " + (resource != null ? resource.getName() : ("Resource #" + id)) +
                    " (" + totalSlots + " slot(s) across " + totalDays + " day(s))";
            StringBuilder details = new StringBuilder();
            if (request.getDays() != null) {
                for (top.ajasta.AjastaApp.reservation.dtos.BookMultiRequest.Day day : request.getDays()) {
                    details.append("Date: ").append(safe(day.getDate())).append("\n");
                    if (day.getSlots() != null) {
                        for (top.ajasta.AjastaApp.reservation.dtos.BookBatchRequest.Slot s : day.getSlots()) {
                            details.append("- ")
                                    .append(s.getStartTime()).append(" - ").append(s.getEndTime())
                                    .append(" | Unit ").append(s.getUnit() == null ? 1 : s.getUnit())
                                    .append("\n");
                        }
                    }
                }
            }
            details.append("Total slots: ").append(totalSlots).append("\n")
                    .append("Price per slot: ").append(pricePerSlot).append("\n")
                    .append("Total: ").append(totalAmount);
            orderService.setCurrentBookingResourceId(id);
            orderService.createBookingOrder(totalAmountBD, bookingTitle, details.toString());
        } catch (Exception ignored) {}

        int totalDays = request.getDays() == null ? 0 : request.getDays().size();
        return ResponseEntity.ok(Response.builder()
                .statusCode(200)
                .message("Your booking has been received for " + totalSlots + " slot(s) across " + totalDays + " day(s). We've sent a secure payment link to your email.")
                .build());
    }
}
