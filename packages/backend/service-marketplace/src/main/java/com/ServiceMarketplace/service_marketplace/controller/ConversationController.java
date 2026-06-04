package com.ServiceMarketplace.service_marketplace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.ConversationResponse;
import com.ServiceMarketplace.service_marketplace.dto.MessageResponse;
import com.ServiceMarketplace.service_marketplace.dto.NotificationResponse;
import com.ServiceMarketplace.service_marketplace.dto.SendMessageRequest;
import com.ServiceMarketplace.service_marketplace.dto.StartConversationRequest;
import com.ServiceMarketplace.service_marketplace.dto.UnreadCountResponse;
import com.ServiceMarketplace.service_marketplace.service.ConversationService;
import com.ServiceMarketplace.service_marketplace.service.NotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ConversationController {

    private final ConversationService conversationService;
    private final NotificationService notificationService;

    public ConversationController(ConversationService conversationService,
            NotificationService notificationService) {
        this.conversationService = conversationService;
        this.notificationService = notificationService;
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> startConversation(
            @Valid @RequestBody StartConversationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(conversationService.startConversation(request, userDetails));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(conversationService.getConversations(userDetails));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(conversationService.getMessages(conversationId, userDetails));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.sendMessage(conversationId, request, userDetails));
    }

    @PostMapping("/conversations/{conversationId}/messages/{messageId}/accept")
    public ResponseEntity<MessageResponse> acceptOffer(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(conversationService.acceptOffer(conversationId, messageId, userDetails));
    }

    @PostMapping("/conversations/{conversationId}/messages/{messageId}/reject")
    public ResponseEntity<MessageResponse> rejectOffer(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(conversationService.rejectOffer(conversationId, messageId, userDetails));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markNotificationRead(
            @PathVariable String notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.markRead(notificationId, userDetails));
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllNotificationsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllRead(userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-counts")
    public ResponseEntity<UnreadCountResponse> getUnreadCounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        long unreadMessages = conversationService.getUnreadMessageCount(userDetails);
        long unreadNotifications = notificationService.getUnreadNotificationCount(userDetails);
        return ResponseEntity.ok(new UnreadCountResponse(unreadMessages, unreadNotifications));
    }
}
