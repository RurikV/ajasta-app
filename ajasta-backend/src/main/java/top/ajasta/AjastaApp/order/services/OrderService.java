package top.ajasta.AjastaApp.order.services;

import top.ajasta.AjastaApp.enums.OrderStatus;
import top.ajasta.AjastaApp.order.dtos.OrderDTO;
import top.ajasta.AjastaApp.order.dtos.OrderItemDTO;
import top.ajasta.AjastaApp.response.Response;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {

    Response<?> placeOrderFromCart();
    Response<OrderDTO> getOrderById(Long id);
    Response<Page<OrderDTO>> getAllOrders(OrderStatus orderStatus, int page, int size);
    Response<List<OrderDTO>> getOrdersOfUser();
    Response<OrderItemDTO> getOrderItemById(Long orderItemId);
    Response<OrderDTO> updateOrderStatus(OrderDTO orderDTO);
    Response<Long> countUniqueCustomers();
    Response<?> deleteOwnOrder(Long id);
}
