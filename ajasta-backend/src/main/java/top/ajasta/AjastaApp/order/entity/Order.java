package top.ajasta.AjastaApp.order.entity;


import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.enums.OrderStatus;
import top.ajasta.AjastaApp.enums.PaymentStatus;
import top.ajasta.AjastaApp.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Customer

    private LocalDateTime orderDate;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToOne(mappedBy = "order")
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    // Booking metadata for resource bookings (optional)
    private Boolean booking; // true if this order represents a resource booking

    private String bookingTitle; // e.g., "Booking: City Turf Court A"

    @Column(length = 4000)
    private String bookingDetails; // human-readable details (dates, times, units)

    // Resource association (nullable): the resource this booking/order belongs to
    @Column(name = "resource_id")
    private Long resourceId;

}







