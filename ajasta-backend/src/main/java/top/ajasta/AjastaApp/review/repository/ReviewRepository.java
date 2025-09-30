package top.ajasta.AjastaApp.review.repository;

import top.ajasta.AjastaApp.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByResourceIdOrderByIdDesc(Long resourceId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.resource.id = :resourceId")
    Double calculateAverageRatingByResourceId(@Param("resourceId") Long resourceId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Review r " +
            "WHERE r.user.id = :userId AND r.resource.id = :resourceId AND r.orderId = :orderId")
    boolean existsByUserIdAndResourceIdAndOrderId(
            @Param("userId") Long userId,
            @Param("resourceId") Long resourceId,
            @Param("orderId") Long orderId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r WHERE r.user.id = :userId AND r.resource.id = :resourceId")
    boolean existsByUserIdAndResourceId(@Param("userId") Long userId, @Param("resourceId") Long resourceId);
}
