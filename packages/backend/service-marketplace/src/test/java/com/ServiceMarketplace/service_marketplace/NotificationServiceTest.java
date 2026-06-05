package com.ServiceMarketplace.service_marketplace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;

import com.ServiceMarketplace.service_marketplace.dto.NotificationResponse;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.AppNotification;
import com.ServiceMarketplace.service_marketplace.model.NotificationType;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.AppNotificationRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private AppNotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private NotificationService notificationService;

    private User recipient;

    @BeforeEach
    void setUp() {
        recipient = new User();
        recipient.setId("user-1");
        recipient.setEmail("user@calpoly.edu");
        recipient.setFirstName("Alice");
        recipient.setLastName("Smith");

        lenient().when(userDetails.getUsername()).thenReturn("user@calpoly.edu");
        lenient().when(userRepository.findByEmail("user@calpoly.edu")).thenReturn(Optional.of(recipient));
    }

    // --- send ---

    @Test
    void send_savesNotificationAndPushesToRecipientOverWebSocket() {
        AppNotification saved = buildNotification("notif-1", "user-1", NotificationType.NEW_MESSAGE, false);

        when(notificationRepository.save(any(AppNotification.class))).thenReturn(saved);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(recipient));

        notificationService.send("user-1", NotificationType.NEW_MESSAGE, "New Message", "You have a new message", "conv-1");

        verify(notificationRepository).save(any(AppNotification.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq("user@calpoly.edu"), eq("/queue/notifications"), any(NotificationResponse.class));
    }

    @Test
    void send_recipientNotFound_savesButDoesNotPushWebSocket() {
        AppNotification saved = buildNotification("notif-1", "user-unknown", NotificationType.NEW_MESSAGE, false);

        when(notificationRepository.save(any(AppNotification.class))).thenReturn(saved);
        when(userRepository.findById("user-unknown")).thenReturn(Optional.empty());

        notificationService.send("user-unknown", NotificationType.NEW_MESSAGE, "New Message", "Body", "ref-1");

        verify(notificationRepository).save(any(AppNotification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    // --- getNotifications ---

    @Test
    void getNotifications_returnsAllForUserInDescendingOrder() {
        AppNotification n1 = buildNotification("n1", "user-1", NotificationType.BOOKING_REQUESTED, false);
        AppNotification n2 = buildNotification("n2", "user-1", NotificationType.BOOKING_CONFIRMED, true);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(n1, n2));

        List<NotificationResponse> result = notificationService.getNotifications(userDetails);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("n1");
        assertThat(result.get(0).getType()).isEqualTo(NotificationType.BOOKING_REQUESTED);
        assertThat(result.get(0).isRead()).isFalse();
        assertThat(result.get(1).isRead()).isTrue();
    }

    // --- markRead ---

    @Test
    void markRead_ownNotification_marksAsRead() {
        AppNotification notif = buildNotification("notif-1", "user-1", NotificationType.NEW_MESSAGE, false);

        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notif));
        when(notificationRepository.save(notif)).thenReturn(notif);

        NotificationResponse result = notificationService.markRead("notif-1", userDetails);

        assertThat(result.isRead()).isTrue();
        assertThat(notif.isRead()).isTrue();
        verify(notificationRepository).save(notif);
    }

    @Test
    void markRead_notifBelongingToOtherUser_throwsAccessDeniedException() {
        AppNotification notif = buildNotification("notif-1", "other-user", NotificationType.NEW_MESSAGE, false);

        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> notificationService.markRead("notif-1", userDetails))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void markRead_notificationNotFound_throwsResourceNotFoundException() {
        when(notificationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead("missing", userDetails))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- markAllRead ---

    @Test
    void markAllRead_setsAllNotificationsToRead() {
        AppNotification n1 = buildNotification("n1", "user-1", NotificationType.BOOKING_REQUESTED, false);
        AppNotification n2 = buildNotification("n2", "user-1", NotificationType.NEW_MESSAGE, false);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(n1, n2));

        notificationService.markAllRead(userDetails);

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }

    @Test
    void markAllRead_noNotifications_doesNotSave() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of());

        notificationService.markAllRead(userDetails);

        verify(notificationRepository).saveAll(List.of());
    }

    // --- getUnreadNotificationCount ---

    @Test
    void getUnreadNotificationCount_returnsRepositoryCount() {
        when(notificationRepository.countByUserIdAndReadFalse("user-1")).thenReturn(5L);

        long count = notificationService.getUnreadNotificationCount(userDetails);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void getUnreadNotificationCount_noUnread_returnsZero() {
        when(notificationRepository.countByUserIdAndReadFalse("user-1")).thenReturn(0L);

        long count = notificationService.getUnreadNotificationCount(userDetails);

        assertThat(count).isZero();
    }

    // --- helpers ---

    private AppNotification buildNotification(String id, String userId, NotificationType type, boolean read) {
        AppNotification n = new AppNotification();
        n.setId(id);
        n.setUserId(userId);
        n.setType(type);
        n.setTitle("Title");
        n.setBody("Body");
        n.setReferenceId("ref-1");
        n.setRead(read);
        n.setCreatedAt(Instant.now());
        return n;
    }
}
