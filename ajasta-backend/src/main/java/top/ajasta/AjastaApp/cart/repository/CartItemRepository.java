package top.ajasta.AjastaApp.cart.repository;

import top.ajasta.AjastaApp.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
