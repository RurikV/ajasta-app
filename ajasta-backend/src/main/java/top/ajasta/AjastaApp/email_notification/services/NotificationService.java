package top.ajasta.AjastaApp.email_notification.services;

import top.ajasta.AjastaApp.email_notification.dtos.NotificationDTO;

public interface NotificationService {
    void sendEmail(NotificationDTO notificationDTO);
}
