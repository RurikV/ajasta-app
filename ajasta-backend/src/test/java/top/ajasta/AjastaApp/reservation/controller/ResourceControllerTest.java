package top.ajasta.AjastaApp.reservation.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.TemplateEngine;
import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.email_notification.dtos.NotificationDTO;
import top.ajasta.AjastaApp.email_notification.services.NotificationService;
import top.ajasta.AjastaApp.order.services.OrderService;
import top.ajasta.AjastaApp.reservation.dtos.BookBatchRequest;
import top.ajasta.AjastaApp.reservation.dtos.BookMultiRequest;
import top.ajasta.AjastaApp.reservation.dtos.ResourceDTO;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import top.ajasta.AjastaApp.reservation.services.ResourceService;
import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.auth_users.services.UserService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ResourceControllerTest {

    private final ResourceService resourceService = org.mockito.Mockito.mock(ResourceService.class);
    private final NotificationService notificationService = org.mockito.Mockito.mock(NotificationService.class);
    private final UserService userService = org.mockito.Mockito.mock(UserService.class);
    private final TemplateEngine templateEngine = org.mockito.Mockito.mock(TemplateEngine.class);
    private final OrderService orderService = org.mockito.Mockito.mock(OrderService.class);

    private ResourceController controller() throws Exception {
        ResourceController c = new ResourceController(resourceService, notificationService, userService, templateEngine, orderService);
        java.lang.reflect.Field f = ResourceController.class.getDeclaredField("basePaymentLink");
        f.setAccessible(true);
        f.set(c, "https://pay.example/?order=");
        return c;
    }

    private ResourceDTO makeResource() {
        return ResourceDTO.builder()
                .id(1L)
                .name("Court A")
                .type(ResourceType.TURF_COURT)
                .location("Center")
                .pricePerSlot(new BigDecimal("15.00"))
                .build();
    }

    private User makeUser() {
        User u = new User();
        u.setName("John");
        u.setEmail("john@example.com");
        return u;
    }

    @Test
    void bookBatch_sendsSingleEmail_andCreatesOrder_withAggregatedTotal() throws Exception {
        // Arrange
        given(resourceService.getResourceById(1L))
                .willReturn(Response.<ResourceDTO>builder().statusCode(200).data(makeResource()).build());
        given(userService.getCurrentLoggedInUser()).willReturn(makeUser());
        given(templateEngine.process(eq("booking-confirmation"), any())).willReturn("<html></html>");

        BookBatchRequest req = new BookBatchRequest();
        req.setDate("2025-01-10");
        BookBatchRequest.Slot s1 = new BookBatchRequest.Slot();
        s1.setStartTime("09:00"); s1.setEndTime("09:30"); s1.setUnit(1);
        BookBatchRequest.Slot s2 = new BookBatchRequest.Slot();
        s2.setStartTime("09:30"); s2.setEndTime("10:00"); s2.setUnit(1);
        req.setSlots(Arrays.asList(s1, s2));

        // Act
        var responseEntity = controller().bookBatch(1L, req);

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(200, responseEntity.getStatusCodeValue());
        String msg = Objects.requireNonNull(responseEntity.getBody()).getMessage();
        org.junit.jupiter.api.Assertions.assertTrue(msg.contains("2 slot(s)"));
        org.junit.jupiter.api.Assertions.assertTrue(msg.toLowerCase().contains("email"));

        // Verify one email was sent
        ArgumentCaptor<NotificationDTO> cap = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(notificationService, times(1)).sendEmail(cap.capture());

        // Verify booking order created with total amount 30.00
        ArgumentCaptor<BigDecimal> amountCap = ArgumentCaptor.forClass(BigDecimal.class);
        verify(orderService, times(1)).createBookingOrder(amountCap.capture(), anyString(), anyString());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("30.00"), amountCap.getValue());
    }

    @Test
    void bookMulti_sendsSingleEmail_andCreatesOrder_withAllDaysAggregated() throws Exception {
        // Arrange
        given(resourceService.getResourceById(1L))
                .willReturn(Response.<ResourceDTO>builder().statusCode(200).data(makeResource()).build());
        given(userService.getCurrentLoggedInUser()).willReturn(makeUser());
        given(templateEngine.process(eq("booking-confirmation"), any())).willReturn("<html></html>");

        BookMultiRequest req = new BookMultiRequest();
        BookMultiRequest.Day d1 = new BookMultiRequest.Day();
        d1.setDate("2025-01-10");
        BookBatchRequest.Slot sd11 = new BookBatchRequest.Slot();
        sd11.setStartTime("09:00"); sd11.setEndTime("09:30"); sd11.setUnit(1);
        d1.setSlots(Arrays.asList(sd11));

        BookMultiRequest.Day d2 = new BookMultiRequest.Day();
        d2.setDate("2025-01-11");
        BookBatchRequest.Slot sd21 = new BookBatchRequest.Slot();
        sd21.setStartTime("09:30"); sd21.setEndTime("10:00"); sd21.setUnit(1);
        BookBatchRequest.Slot sd22 = new BookBatchRequest.Slot();
        sd22.setStartTime("10:00"); sd22.setEndTime("10:30"); sd22.setUnit(1);
        d2.setSlots(Arrays.asList(sd21, sd22));

        req.setDays(Arrays.asList(d1, d2));

        // Act
        var responseEntity = controller().bookMulti(1L, req);

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(200, responseEntity.getStatusCodeValue());
        org.junit.jupiter.api.Assertions.assertTrue(Objects.requireNonNull(responseEntity.getBody()).getMessage().contains("3 slot(s) across 2 day(s)"));

        // Verify one email was sent
        verify(notificationService, times(1)).sendEmail(any(NotificationDTO.class));

        // Verify booking order created with total amount 45.00
        ArgumentCaptor<BigDecimal> amountCap = ArgumentCaptor.forClass(BigDecimal.class);
        verify(orderService, times(1)).createBookingOrder(amountCap.capture(), anyString(), anyString());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("45.00"), amountCap.getValue());
    }
}
