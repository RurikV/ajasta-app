package top.ajasta.AjastaApp.reservation.repository;

import top.ajasta.AjastaApp.reservation.entity.Resource;
import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByActiveTrue();
    List<Resource> findByType(ResourceType type);
    List<Resource> findByNameContainingIgnoreCase(String name);

    // Find resources managed by a specific user
    List<Resource> findByManagers_Id(Long userId);
}
