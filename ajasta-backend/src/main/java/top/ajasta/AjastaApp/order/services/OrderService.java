package top.ajasta.AjastaApp.order.services;

import top.ajasta.AjastaApp.enums.OrderStatus;
import top.ajasta.AjastaApp.order.dtos.OrderDTO;
import top.ajasta.AjastaApp.order.dtos.OrderItemDTO;
import top.ajasta.AjastaApp.response.Response;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    Response<OrderDTO> getOrderById(Long id);
    Response<Page<OrderDTO>> getAllOrders(OrderStatus orderStatus, int page, int size, String name);
    Response<List<OrderDTO>> getOrdersOfUser();
    Response<OrderItemDTO> getOrderItemById(Long orderItemId);
    Response<OrderDTO> updateOrderStatus(OrderDTO orderDTO);
    Response<Long> countUniqueCustomers();
    Response<?> deleteOwnOrder(Long id);

    // Create a simple order entry for a resource booking (no items)
    void createBookingOrder(BigDecimal totalAmount, String bookingTitle, String bookingDetails);

    // Set resource context for subsequent booking order creation
    void setCurrentBookingResourceId(Long resourceId);
}
