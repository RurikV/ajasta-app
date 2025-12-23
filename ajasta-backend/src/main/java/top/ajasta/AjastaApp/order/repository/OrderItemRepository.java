package top.ajasta.AjastaApp.order.repository;

import top.ajasta.AjastaApp.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
