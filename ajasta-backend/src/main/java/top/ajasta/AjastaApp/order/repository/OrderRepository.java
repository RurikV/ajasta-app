package top.ajasta.AjastaApp.order.repository;

import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.enums.OrderStatus;
import top.ajasta.AjastaApp.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    List<Order> findByUserOrderByOrderDateDesc(User user);

    @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o")
    long countDistinctUsers();

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o " +
           "WHERE o.user.id = :userId AND o.booking = true AND (:keyword IS NULL OR LOWER(o.bookingTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    boolean userHasBookingWithTitleLike(@Param("userId") Long userId, @Param("keyword") String keyword);

    // Scoped queries for resource managers
    Page<Order> findByResourceIdIn(List<Long> resourceIds, Pageable pageable);

    Page<Order> findByOrderStatusAndResourceIdIn(OrderStatus orderStatus, List<Long> resourceIds, Pageable pageable);

    // Fallback queries for legacy booking orders without resourceId
    Page<Order> findByResourceIdIsNullAndBookingTrue(Pageable pageable);

    Page<Order> findByResourceIdIsNullAndBookingTrueAndOrderStatus(OrderStatus orderStatus, Pageable pageable);
}
