package top.ajasta.AjastaApp.email_notification.repository;

import top.ajasta.AjastaApp.email_notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
