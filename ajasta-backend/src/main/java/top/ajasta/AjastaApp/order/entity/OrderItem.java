package top.ajasta.AjastaApp.order.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "order_items")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Snapshot fields retained after removal of Menu entity
    private String itemName;
    @Column(length = 2000)
    private String itemDescription;
    private String itemImageUrl;

    private int quantity;

    private BigDecimal pricePerUnit;
    private BigDecimal subtotal;

}
