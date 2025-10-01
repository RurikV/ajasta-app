package top.ajasta.AjastaApp.order.services;

import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.auth_users.services.UserService;
import top.ajasta.AjastaApp.email_notification.dtos.NotificationDTO;
import top.ajasta.AjastaApp.email_notification.services.NotificationService;
import top.ajasta.AjastaApp.enums.OrderStatus;
import top.ajasta.AjastaApp.enums.PaymentStatus;
import top.ajasta.AjastaApp.exceptions.BadRequestException;
import top.ajasta.AjastaApp.exceptions.NotFoundException;
import top.ajasta.AjastaApp.order.dtos.OrderDTO;
import top.ajasta.AjastaApp.order.dtos.OrderItemDTO;
import top.ajasta.AjastaApp.order.entity.Order;
import top.ajasta.AjastaApp.order.entity.OrderItem;
import top.ajasta.AjastaApp.order.repository.OrderItemRepository;
import top.ajasta.AjastaApp.order.repository.OrderRepository;
import top.ajasta.AjastaApp.payment.repository.PaymentRepository;
import top.ajasta.AjastaApp.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl  implements OrderService{


    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final TemplateEngine templateEngine;
    private final PaymentRepository paymentRepository;
    private final top.ajasta.AjastaApp.reservation.repository.ResourceRepository resourceRepository;

    private static final ThreadLocal<Long> CURRENT_BOOKING_RESOURCE_ID = new ThreadLocal<>();

    @Value("${base.payment.link}")
    private String basePaymentLink;



    @Override
    public Response<OrderDTO> getOrderById(Long id) {

        log.info("Inside getOrderById()");
        Order order = orderRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Order Not Found"));

        // Authorization: Admin can view any, Resource Manager only if manages the resource
        User current = userService.getCurrentLoggedInUser();
        boolean isAdmin = current.getRoles() != null && current.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));
        if (!isAdmin) {
            boolean isRM = current.getRoles() != null && current.getRoles().stream().anyMatch(r -> "RESOURCE_MANAGER".equalsIgnoreCase(r.getName()));
            if (isRM) {
                Long rid = order.getResourceId();
                if (rid == null) {
                    throw new top.ajasta.AjastaApp.exceptions.UnauthorizedAccessException("Not allowed to view this order");
                }
                List<Long> managedIds = resourceRepository.findByManagers_Id(current.getId())
                        .stream().map(top.ajasta.AjastaApp.reservation.entity.Resource::getId).toList();
                if (!managedIds.contains(rid)) {
                    throw new top.ajasta.AjastaApp.exceptions.UnauthorizedAccessException("Not allowed to view this order");
                }
            } else {
                // For other roles, deny (customers have dedicated endpoints)
                throw new top.ajasta.AjastaApp.exceptions.UnauthorizedAccessException("Not allowed to view this order");
            }
        }

        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);

        return Response.<OrderDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(orderDTO)
                .build();
    }

    @Override
    public Response<Page<OrderDTO>> getAllOrders(OrderStatus orderStatus, int page, int size) {
        log.info("Inside getAllOrders()");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // Determine caller role and scope
        User current = userService.getCurrentLoggedInUser();
        boolean isAdmin = current.getRoles() != null && current.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));

        Page<Order> orderPage;
        if (isAdmin) {
            if (orderStatus != null){
                orderPage = orderRepository.findByOrderStatus(orderStatus, pageable);
            } else {
                orderPage = orderRepository.findAll(pageable);
            }
        } else {
            // Resource Manager: restrict to orders belonging to resources they manage
            List<Long> managedIds = resourceRepository.findByManagers_Id(current.getId())
                    .stream().map(top.ajasta.AjastaApp.reservation.entity.Resource::getId).toList();
            if (managedIds.isEmpty()) {
                orderPage = Page.empty(pageable);
            } else {
                if (orderStatus != null) {
                    orderPage = orderRepository.findByOrderStatusAndResourceIdIn(orderStatus, managedIds, pageable);
                } else {
                    orderPage = orderRepository.findByResourceIdIn(managedIds, pageable);
                }
            }
        }

        Page<OrderDTO> orderDTOPage  = orderPage.map(order -> modelMapper.map(order, OrderDTO.class));

        return Response.<Page<OrderDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .data(orderDTOPage)
                .build();

    }

    @Override
    public Response<List<OrderDTO>> getOrdersOfUser() {
        log.info("Inside getOrdersOfUser()");

        User customer = userService.getCurrentLoggedInUser();
        List<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(customer);

        List<OrderDTO> orderDTOS = orders.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .toList();

        orderDTOS.forEach(orderItem -> {
            orderItem.setUser(null);
        });


        return Response.<List<OrderDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Orders for user retrieved successfully")
                .data(orderDTOS)
                .build();

    }

    @Override
    public void setCurrentBookingResourceId(Long resourceId) {
        if (resourceId == null) {
            CURRENT_BOOKING_RESOURCE_ID.remove();
        } else {
            CURRENT_BOOKING_RESOURCE_ID.set(resourceId);
        }
    }

    @Override
    public Response<OrderItemDTO> getOrderItemById(Long orderItemId) {

        log.info("Inside getOrderItemById()");

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(()-> new NotFoundException("Order Item Not Found"));


        OrderItemDTO orderItemDTO = modelMapper.map(orderItem, OrderItemDTO.class);

        return Response.<OrderItemDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("OrderItem retrieved successfully")
                .data(orderItemDTO)
                .build();

    }

    @Override
    public Response<OrderDTO> updateOrderStatus(OrderDTO orderDTO) {
        log.info("Inside updateOrderStatus()");

        Order order = orderRepository.findById(orderDTO.getId())
                .orElseThrow(() -> new NotFoundException("Order not found: "));

        // Authorization: Admin or assigned Resource Manager only
        User current = userService.getCurrentLoggedInUser();
        boolean isAdmin = current.getRoles() != null && current.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));
        if (!isAdmin) {
            boolean isRM = current.getRoles() != null && current.getRoles().stream().anyMatch(r -> "RESOURCE_MANAGER".equalsIgnoreCase(r.getName()));
            if (!isRM) {
                throw new top.ajasta.AjastaApp.exceptions.UnauthorizedAccessException("Not allowed to update this order");
            }
            Long rid = order.getResourceId();
            if (rid == null) {
                throw new top.ajasta.AjastaApp.exceptions.UnauthorizedAccessException("Not allowed to update this order");
            }
            List<Long> managedIds = resourceRepository.findByManagers_Id(current.getId())
                    .stream().map(top.ajasta.AjastaApp.reservation.entity.Resource::getId).toList();
            if (!managedIds.contains(rid)) {
                throw new top.ajasta.AjastaApp.exceptions.UnauthorizedAccessException("Not allowed to update this order");
            }
        }

        OrderStatus orderStatus = orderDTO.getOrderStatus();
        order.setOrderStatus(orderStatus);

        orderRepository.save(order);

        return Response.<OrderDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order status updated successfully")
                .build();
    }

    @Override
    @Transactional
    public Response<?> deleteOwnOrder(Long id) {
        log.info("Inside deleteOwnOrder()");
        User customer = userService.getCurrentLoggedInUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order Not Found"));

        if (order.getUser() == null || !order.getUser().getId().equals(customer.getId())) {
            throw new BadRequestException("You are not allowed to delete this order");
        }

        // Delete payment first if exists to avoid FK constraint issues
        if (order.getPayment() != null) {
            try {
                paymentRepository.delete(order.getPayment());
            } catch (Exception e) {
                log.warn("Failed to delete payment for order {}: {}", id, e.getMessage());
            }
            order.setPayment(null);
        }

        orderRepository.delete(order);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order deleted successfully")
                .build();
    }

    @Override
    public Response<Long> countUniqueCustomers() {
        log.info("Inside countUniqueCustomers()");

        long uniqueCustomerCount = orderRepository.countDistinctUsers();
        return Response.<Long>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Unique customer count retrieved successfully")
                .data(uniqueCustomerCount)
                .build();
    }

    @Override
    @Transactional
    public void createBookingOrder(BigDecimal totalAmount, String bookingTitle, String bookingDetails) {
        log.info("Inside createBookingOrder() amount={}, title={}...", totalAmount, bookingTitle);
        User customer = userService.getCurrentLoggedInUser();

        Long rid = CURRENT_BOOKING_RESOURCE_ID.get();
        try {
            Order order = Order.builder()
                    .user(customer)
                    .orderDate(LocalDateTime.now())
                    .totalAmount(totalAmount == null ? BigDecimal.ZERO : totalAmount)
                    .orderStatus(OrderStatus.INITIALIZED)
                    .paymentStatus(PaymentStatus.PENDING)
                    .orderItems(new ArrayList<>())
                    .booking(Boolean.TRUE)
                    .bookingTitle(bookingTitle)
                    .bookingDetails(bookingDetails)
                    .resourceId(rid)
                    .build();

            Order saved = orderRepository.save(order);
            OrderDTO dto = modelMapper.map(saved, OrderDTO.class);

            Response.<OrderDTO>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Booking recorded in order history")
                    .data(dto)
                    .build();
        } finally {
            // Clear context to avoid leakage across requests
            CURRENT_BOOKING_RESOURCE_ID.remove();
        }
    }



    private void sendOrderConfirmationEmail(User customer, OrderDTO orderDTO){

        String subject =  "Your Order Confirmation - Order #" + orderDTO.getId();

        //create a Thymeleaf contex and set variables. import the context from Thymeleaf
        Context context = new Context(Locale.getDefault());

        context.setVariable("customerName", customer.getName());
        context.setVariable("orderId", String.valueOf(orderDTO.getId()));
        context.setVariable("orderDate", orderDTO.getOrderDate().toString());
        context.setVariable("totalAmount", orderDTO.getTotalAmount().toString());

        // Format address
        String address = orderDTO.getUser().getAddress();
        context.setVariable("address", address);

        context.setVariable("currentYear", java.time.Year.now());

        // Build the order items HTML using StringBuilder
        StringBuilder orderItemsHtml = new StringBuilder();

        for (OrderItemDTO item : orderDTO.getOrderItems()) {
            orderItemsHtml.append("<div class=\"order-item\">")
                    .append("<p>")
                    .append(item.getItemName() != null ? item.getItemName() : "Item")
                    .append(" x ").append(item.getQuantity()).append("</p>")
                    .append("<p> $ ").append(item.getSubtotal()).append("</p>")
                    .append("</div>");
        }

            context.setVariable("orderItemsHtml", orderItemsHtml.toString());
            context.setVariable("totalItems", orderDTO.getOrderItems().size());


            String paymentLink = basePaymentLink + orderDTO.getId() + "&amount=" + orderDTO.getTotalAmount(); // Replace "yourdomain.com"
            context.setVariable("paymentLink", paymentLink);

            // Process the Thymeleaf template to generate the HTML email body
            String emailBody = templateEngine.process("order-confirmation", context);  // "order-confirmation" is the template name

            notificationService.sendEmail(NotificationDTO.builder()
                    .recipient(customer.getEmail())
                    .subject(subject)
                    .body(emailBody)
                    .isHtml(true)
                    .build());

        }

}












