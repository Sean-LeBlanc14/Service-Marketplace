package com.ServiceMarketplace.service_marketplace.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.NotificationResponse;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.AppNotification;
import com.ServiceMarketplace.service_marketplace.model.NotificationType;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.AppNotificationRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;

@Service
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(AppNotificationRepository notificationRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void send(String recipientUserId, NotificationType type, String title, String body, String referenceId) {
        AppNotification notification = new AppNotification();
        notification.setUserId(recipientUserId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setReferenceId(referenceId);
        notification.setRead(false);

        AppNotification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);

        String recipientEmail = userRepository.findById(recipientUserId)
                .map(u -> u.getEmail())
                .orElse(null);

        if (recipientEmail != null) {
            messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/notifications", response);
        }
    }

    public List<NotificationResponse> getNotifications(UserDetails userDetails) {
        User user = resolveUser(userDetails);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NotificationResponse markRead(String notificationId, UserDetails userDetails) {
        User user = resolveUser(userDetails);
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUserId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot mark this notification as read");
        }

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    public void markAllRead(UserDetails userDetails) {
        User user = resolveUser(userDetails);
        List<AppNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    public long getUnreadNotificationCount(UserDetails userDetails) {
        User user = resolveUser(userDetails);
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private NotificationResponse toResponse(AppNotification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt());
    }
}
