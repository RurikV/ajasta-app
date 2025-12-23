package top.ajasta.AjastaApp.order.controller;


import top.ajasta.AjastaApp.enums.OrderStatus;
import top.ajasta.AjastaApp.order.dtos.OrderDTO;
import top.ajasta.AjastaApp.order.dtos.OrderItemDTO;
import top.ajasta.AjastaApp.order.services.OrderService;
import top.ajasta.AjastaApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','RESOURCE_MANAGER')")
    public ResponseEntity<Response<OrderDTO>> getOrderById(@PathVariable Long id){
        return ResponseEntity.ok(orderService.getOrderById(id));
    }


    @GetMapping("/me")
    public ResponseEntity<Response<List<OrderDTO>>> getMyOrders(){
        return ResponseEntity.ok(orderService.getOrdersOfUser());
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<Response<OrderItemDTO>> getOrderItemById(@PathVariable Long orderItemId) {
        return ResponseEntity.ok(orderService.getOrderItemById(orderItemId));
    }


    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ADMIN','RESOURCE_MANAGER')")
    public ResponseEntity<Response<Page<OrderDTO>>> getAllOrders(
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(orderService.getAllOrders(orderStatus, page, size, name));
    }


    @PutMapping("/update")
    @PreAuthorize("hasAnyAuthority('ADMIN','RESOURCE_MANAGER')")
    public ResponseEntity<Response<OrderDTO>> updateOrderStatus(@RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderDTO));
    }


    @GetMapping("/unique-customers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<Long>> countUniqueCustomers() {
        return ResponseEntity.ok(orderService.countUniqueCustomers());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<?>> deleteOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.deleteOwnOrder(id));
    }

}
