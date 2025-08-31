package top.ajasta.AjastaApp.category.repository;

import top.ajasta.AjastaApp.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}
