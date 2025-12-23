package top.ajasta.AjastaApp.order.dtos;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import top.ajasta.AjastaApp.auth_users.dtos.UserDTO;
import top.ajasta.AjastaApp.enums.OrderStatus;
import top.ajasta.AjastaApp.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDTO {


    private Long id;

    private LocalDateTime orderDate;

    private BigDecimal totalAmount;

    private OrderStatus orderStatus;

    private PaymentStatus paymentStatus;

    private UserDTO user; // Customer who is making/made the order

    private List<OrderItemDTO> orderItems;

    // Booking metadata mirrored from Order entity
    private Boolean booking;
    private String bookingTitle;
    private String bookingDetails;

    // Resource association (nullable)
    private Long resourceId;
}
