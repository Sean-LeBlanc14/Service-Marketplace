package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.ConversationResponse;
import com.ServiceMarketplace.service_marketplace.dto.MessageResponse;
import com.ServiceMarketplace.service_marketplace.dto.SendMessageRequest;
import com.ServiceMarketplace.service_marketplace.dto.StartConversationRequest;
import com.ServiceMarketplace.service_marketplace.exception.ConversationNotFoundException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.exception.UnauthorizedChatAccessException;
import com.ServiceMarketplace.service_marketplace.model.Conversation;
import com.ServiceMarketplace.service_marketplace.model.Message;
import com.ServiceMarketplace.service_marketplace.model.MessageType;
import com.ServiceMarketplace.service_marketplace.model.NotificationType;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.ConversationRepository;
import com.ServiceMarketplace.service_marketplace.repository.MessageRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public ConversationService(ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            ServiceRepository serviceRepository,
            SimpMessagingTemplate messagingTemplate,
            NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    public ConversationResponse startConversation(StartConversationRequest request, UserDetails userDetails) {
        User customer = getUser(userDetails);

        com.ServiceMarketplace.service_marketplace.model.Service service = serviceRepository
                .findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service", request.getServiceId()));

        if (service.getUserId().equals(customer.getId())) {
            throw new UnauthorizedChatAccessException("You cannot start a conversation about your own service");
        }

        return conversationRepository
                .findByServiceIdAndCustomerIdAndProviderId(request.getServiceId(), customer.getId(), service.getUserId())
                .map(existing -> toResponse(existing, customer.getId()))
                .orElseGet(() -> {
                    User provider = userRepository.findById(service.getUserId())
                            .orElseThrow(() -> new ResourceNotFoundException("Provider", service.getUserId()));

                    Conversation conversation = new Conversation();
                    conversation.setServiceId(service.getId());
                    conversation.setServiceTitle(service.getTitle());
                    conversation.setCustomerId(customer.getId());
                    conversation.setProviderId(service.getUserId());
                    conversation.setLastMessageAt(Instant.now());

                    Conversation saved = conversationRepository.save(conversation);
                    return toResponseWithNames(saved, customer, provider, customer.getId());
                });
    }

    public List<ConversationResponse> getConversations(UserDetails userDetails) {
        User user = getUser(userDetails);
        return conversationRepository
                .findByCustomerIdOrProviderIdOrderByLastMessageAtDesc(user.getId(), user.getId())
                .stream()
                .map(c -> toResponse(c, user.getId()))
                .toList();
    }

    public List<MessageResponse> getMessages(String conversationId, UserDetails userDetails) {
        User user = getUser(userDetails);
        Conversation conversation = getConversation(conversationId);
        assertParticipant(conversation, user.getId());

        List<Message> unread = messageRepository
                .findByConversationIdAndSenderIdNotAndReadFalse(conversationId, user.getId());
        unread.forEach(m -> m.setRead(true));
        if (!unread.isEmpty()) {
            messageRepository.saveAll(unread);
        }

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    public MessageResponse sendMessage(String conversationId, SendMessageRequest request, UserDetails userDetails) {
        User sender = getUser(userDetails);
        Conversation conversation = getConversation(conversationId);
        assertParticipant(conversation, sender.getId());

        if (request.getType() == MessageType.PRICE_OFFER) {
            if (!sender.getId().equals(conversation.getCustomerId())) {
                throw new UnauthorizedChatAccessException("Only the customer can send price offers");
            }
            if (request.getOfferedPrice() == null || request.getOfferedPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price offer must include a positive price");
            }
        } else if (request.getType() == MessageType.TEXT) {
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new IllegalArgumentException("Message content cannot be empty");
            }
        } else {
            throw new IllegalArgumentException("Use the accept or reject endpoint to respond to price offers");
        }

        Message message = buildMessage(conversationId, sender, request.getType(),
                request.getContent(), request.getOfferedPrice(), false);
        Message saved = messageRepository.save(message);

        updateConversationPreview(conversation, buildPreview(saved), Instant.now());

        MessageResponse response = toMessageResponse(saved);
        String recipientId = getRecipientId(conversation, sender.getId());
        pushToRecipient(recipientId, response);

        if (request.getType() == MessageType.PRICE_OFFER) {
            notificationService.send(recipientId, NotificationType.PRICE_OFFER_RECEIVED,
                    "Price Offer Received",
                    sender.getFirstName() + " offered $" + request.getOfferedPrice() + " for " + conversation.getServiceTitle(),
                    conversationId);
        } else {
            notificationService.send(recipientId, NotificationType.NEW_MESSAGE,
                    "New Message from " + sender.getFirstName(),
                    truncate(request.getContent(), 80),
                    conversationId);
        }

        return response;
    }

    public MessageResponse acceptOffer(String conversationId, String messageId, UserDetails userDetails) {
        return respondToOffer(conversationId, messageId, true, userDetails);
    }

    public MessageResponse rejectOffer(String conversationId, String messageId, UserDetails userDetails) {
        return respondToOffer(conversationId, messageId, false, userDetails);
    }

    private MessageResponse respondToOffer(String conversationId, String messageId, boolean accept, UserDetails userDetails) {
        User responder = getUser(userDetails);
        Conversation conversation = getConversation(conversationId);
        assertParticipant(conversation, responder.getId());

        if (!responder.getId().equals(conversation.getProviderId())) {
            throw new UnauthorizedChatAccessException("Only the provider can respond to price offers");
        }

        Message offer = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId));

        if (!offer.getConversationId().equals(conversationId)) {
            throw new UnauthorizedChatAccessException("Message does not belong to this conversation");
        }
        if (offer.getType() != MessageType.PRICE_OFFER) {
            throw new IllegalArgumentException("The referenced message is not a price offer");
        }
        if (offer.isOfferResponded()) {
            throw new IllegalArgumentException("This offer has already been responded to");
        }

        offer.setOfferResponded(true);
        messageRepository.save(offer);

        MessageType responseType = accept ? MessageType.PRICE_ACCEPTED : MessageType.PRICE_REJECTED;
        String content = accept
                ? "Price offer of $" + offer.getOfferedPrice() + " accepted."
                : "Price offer of $" + offer.getOfferedPrice() + " declined.";

        Message responseMessage = buildMessage(conversationId, responder, responseType,
                content, offer.getOfferedPrice(), false);
        Message saved = messageRepository.save(responseMessage);

        updateConversationPreview(conversation, buildPreview(saved), Instant.now());

        MessageResponse response = toMessageResponse(saved);
        pushToRecipient(conversation.getCustomerId(), response);

        NotificationType notifType = accept ? NotificationType.PRICE_OFFER_ACCEPTED : NotificationType.PRICE_OFFER_REJECTED;
        String notifTitle = accept ? "Price Offer Accepted" : "Price Offer Declined";
        String notifBody = accept
                ? responder.getFirstName() + " accepted your $" + offer.getOfferedPrice() + " offer for " + conversation.getServiceTitle()
                : responder.getFirstName() + " declined your $" + offer.getOfferedPrice() + " offer for " + conversation.getServiceTitle();

        notificationService.send(conversation.getCustomerId(), notifType, notifTitle, notifBody, conversationId);

        return response;
    }

    private Message buildMessage(String conversationId, User sender, MessageType type,
            String content, BigDecimal offeredPrice, boolean offerResponded) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(sender.getId());
        message.setSenderName(displayName(sender));
        message.setType(type);
        message.setContent(content);
        message.setOfferedPrice(offeredPrice);
        message.setOfferResponded(offerResponded);
        message.setRead(false);
        return message;
    }

    private void updateConversationPreview(Conversation conversation, String preview, Instant at) {
        conversation.setLastMessagePreview(preview);
        conversation.setLastMessageAt(at);
        conversationRepository.save(conversation);
    }

    private void pushToRecipient(String recipientId, MessageResponse response) {
        userRepository.findById(recipientId).ifPresent(recipient ->
                messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/messages", response));
    }

    private String getRecipientId(Conversation conversation, String senderId) {
        return senderId.equals(conversation.getCustomerId())
                ? conversation.getProviderId()
                : conversation.getCustomerId();
    }

    private String buildPreview(Message message) {
        return switch (message.getType()) {
            case TEXT -> truncate(message.getContent(), 60);
            case PRICE_OFFER -> "Price offer: $" + message.getOfferedPrice();
            case PRICE_ACCEPTED -> "Price offer accepted";
            case PRICE_REJECTED -> "Price offer declined";
        };
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private ConversationResponse toResponse(Conversation c, String currentUserId) {
        User customer = userRepository.findById(c.getCustomerId()).orElse(null);
        User provider = userRepository.findById(c.getProviderId()).orElse(null);
        long unread = messageRepository.countByConversationIdAndSenderIdNotAndReadFalse(c.getId(), currentUserId);

        return new ConversationResponse(
                c.getId(),
                c.getServiceId(),
                c.getServiceTitle(),
                c.getCustomerId(),
                customer != null ? displayName(customer) : c.getCustomerId(),
                c.getProviderId(),
                provider != null ? displayName(provider) : c.getProviderId(),
                c.getLastMessagePreview(),
                c.getLastMessageAt(),
                unread,
                c.getCreatedAt());
    }

    private ConversationResponse toResponseWithNames(Conversation c, User customer, User provider, String currentUserId) {
        return new ConversationResponse(
                c.getId(),
                c.getServiceId(),
                c.getServiceTitle(),
                c.getCustomerId(),
                displayName(customer),
                c.getProviderId(),
                displayName(provider),
                c.getLastMessagePreview(),
                c.getLastMessageAt(),
                0L,
                c.getCreatedAt());
    }

    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getConversationId(),
                m.getSenderId(),
                m.getSenderName(),
                m.getType(),
                m.getContent(),
                m.getOfferedPrice(),
                m.isOfferResponded(),
                m.isRead(),
                m.getCreatedAt());
    }

    public long getUnreadMessageCount(UserDetails userDetails) {
        User user = getUser(userDetails);
        List<String> conversationIds = conversationRepository
                .findByCustomerIdOrProviderIdOrderByLastMessageAtDesc(user.getId(), user.getId())
                .stream()
                .map(Conversation::getId)
                .toList();

        if (conversationIds.isEmpty()) {
            return 0L;
        }

        return messageRepository.countBySenderIdNotAndConversationIdInAndReadFalse(user.getId(), conversationIds);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Conversation getConversation(String id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationNotFoundException(id));
    }

    private void assertParticipant(Conversation conversation, String userId) {
        if (!conversation.getCustomerId().equals(userId) && !conversation.getProviderId().equals(userId)) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }
    }

    private String displayName(User user) {
        String full = (user.getFirstName() + " " + user.getLastName()).trim();
        return full.isBlank() ? user.getEmail() : full;
    }
}
