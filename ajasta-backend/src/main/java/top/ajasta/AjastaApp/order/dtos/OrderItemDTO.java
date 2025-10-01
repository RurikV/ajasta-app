package top.ajasta.AjastaApp.order.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemDTO {

    private Long id;

    private int quantity;

    private BigDecimal pricePerUnit;

    private BigDecimal subtotal;

    // Snapshot of item details at time of order (decoupled from Menu)
    private String itemName;
    private String itemDescription;
    private String itemImageUrl;
}
