package com.platform.hierarchy.service;

import com.platform.hierarchy.model.Notification;
import com.platform.hierarchy.model.User;
import com.platform.hierarchy.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void sendNotification(User user, String message) {
        Notification notification = new Notification(user, message);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));
        
        // Security check
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Access denied. This notification does not belong to you.");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}
