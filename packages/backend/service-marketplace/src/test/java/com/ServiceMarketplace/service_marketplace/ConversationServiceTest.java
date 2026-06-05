package com.ServiceMarketplace.service_marketplace;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.ServiceMarketplace.service_marketplace.dto.ConversationResponse;
import com.ServiceMarketplace.service_marketplace.dto.MessageResponse;
import com.ServiceMarketplace.service_marketplace.dto.SendMessageRequest;
import com.ServiceMarketplace.service_marketplace.dto.StartConversationRequest;
import com.ServiceMarketplace.service_marketplace.exception.ConversationNotFoundException;
import com.ServiceMarketplace.service_marketplace.exception.UnauthorizedChatAccessException;
import com.ServiceMarketplace.service_marketplace.model.Conversation;
import com.ServiceMarketplace.service_marketplace.model.Message;
import com.ServiceMarketplace.service_marketplace.model.MessageType;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.ConversationRepository;
import com.ServiceMarketplace.service_marketplace.repository.MessageRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.ConversationService;
import com.ServiceMarketplace.service_marketplace.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private NotificationService notificationService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private ConversationService conversationService;

    private User customer;
    private User provider;
    private com.ServiceMarketplace.service_marketplace.model.Service service;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId("cust-1");
        customer.setEmail("customer@calpoly.edu");
        customer.setFirstName("Alice");
        customer.setLastName("Student");

        provider = new User();
        provider.setId("prov-1");
        provider.setEmail("provider@calpoly.edu");
        provider.setFirstName("Bob");
        provider.setLastName("Smith");

        service = new com.ServiceMarketplace.service_marketplace.model.Service();
        service.setId("svc-1");
        service.setUserId("prov-1");
        service.setTitle("Math Tutoring");

        conversation = new Conversation();
        conversation.setId("conv-1");
        conversation.setServiceId("svc-1");
        conversation.setServiceTitle("Math Tutoring");
        conversation.setCustomerId("cust-1");
        conversation.setProviderId("prov-1");
        conversation.setLastMessageAt(Instant.now());

        lenient().when(userDetails.getUsername()).thenReturn("customer@calpoly.edu");
    }

    // --- startConversation ---

    @Test
    void startConversation_newConversation_createsAndReturns() {
        StartConversationRequest request = new StartConversationRequest();
        request.setServiceId("svc-1");

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(service));
        when(conversationRepository.findByServiceIdAndCustomerIdAndProviderId("svc-1", "cust-1", "prov-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findById("prov-1")).thenReturn(Optional.of(provider));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId("conv-new");
            return c;
        });

        ConversationResponse result = conversationService.startConversation(request, userDetails);

        assertThat(result.getId()).isEqualTo("conv-new");
        assertThat(result.getServiceTitle()).isEqualTo("Math Tutoring");
        assertThat(result.getCustomerId()).isEqualTo("cust-1");
        assertThat(result.getProviderId()).isEqualTo("prov-1");
        assertThat(result.getCustomerName()).isEqualTo("Alice Student");
        assertThat(result.getProviderName()).isEqualTo("Bob Smith");
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void startConversation_existingConversation_returnsExisting() {
        StartConversationRequest request = new StartConversationRequest();
        request.setServiceId("svc-1");

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(service));
        when(conversationRepository.findByServiceIdAndCustomerIdAndProviderId("svc-1", "cust-1", "prov-1"))
                .thenReturn(Optional.of(conversation));
        when(userRepository.findById("cust-1")).thenReturn(Optional.of(customer));
        when(userRepository.findById("prov-1")).thenReturn(Optional.of(provider));
        when(messageRepository.countByConversationIdAndSenderIdNotAndReadFalse("conv-1", "cust-1"))
                .thenReturn(0L);

        ConversationResponse result = conversationService.startConversation(request, userDetails);

        assertThat(result.getId()).isEqualTo("conv-1");
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void startConversation_ownService_throwsUnauthorizedChatAccessException() {
        StartConversationRequest request = new StartConversationRequest();
        request.setServiceId("svc-1");

        service.setUserId("cust-1");

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(service));

        assertThatThrownBy(() -> conversationService.startConversation(request, userDetails))
                .isInstanceOf(UnauthorizedChatAccessException.class)
                .hasMessageContaining("own service");
    }

    // --- getConversations ---

    @Test
    void getConversations_returnsAllForCurrentUser() {
        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findByCustomerIdOrProviderIdOrderByLastMessageAtDesc("cust-1", "cust-1"))
                .thenReturn(List.of(conversation));
        when(userRepository.findById("cust-1")).thenReturn(Optional.of(customer));
        when(userRepository.findById("prov-1")).thenReturn(Optional.of(provider));
        when(messageRepository.countByConversationIdAndSenderIdNotAndReadFalse("conv-1", "cust-1"))
                .thenReturn(2L);

        List<ConversationResponse> result = conversationService.getConversations(userDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("conv-1");
        assertThat(result.get(0).getUnreadCount()).isEqualTo(2L);
    }

    // --- getMessages ---

    @Test
    void getMessages_participant_returnsMessagesAndMarksRead() {
        Message unread = new Message();
        unread.setId("msg-1");
        unread.setConversationId("conv-1");
        unread.setSenderId("prov-1");
        unread.setType(MessageType.TEXT);
        unread.setContent("Hello");
        unread.setRead(false);

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdAndSenderIdNotAndReadFalse("conv-1", "cust-1"))
                .thenReturn(List.of(unread));
        when(messageRepository.saveAll(any())).thenReturn(List.of(unread));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc("conv-1"))
                .thenReturn(List.of(unread));

        List<MessageResponse> result = conversationService.getMessages("conv-1", userDetails);

        assertThat(result).hasSize(1);
        assertThat(unread.isRead()).isTrue();
        verify(messageRepository).saveAll(any());
    }

    @Test
    void getMessages_nonParticipant_throwsAccessDeniedException() {
        User outsider = new User();
        outsider.setId("other-99");
        outsider.setEmail("outsider@calpoly.edu");

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(outsider));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.getMessages("conv-1", userDetails))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMessages_conversationNotFound_throwsConversationNotFoundException() {
        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getMessages("missing", userDetails))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    // --- sendMessage TEXT ---

    @Test
    void sendMessage_text_savesAndPushesToRecipient() {
        SendMessageRequest request = new SendMessageRequest();
        request.setType(MessageType.TEXT);
        request.setContent("Hi there!");

        Message saved = new Message();
        saved.setId("msg-2");
        saved.setConversationId("conv-1");
        saved.setSenderId("cust-1");
        saved.setSenderName("Alice Student");
        saved.setType(MessageType.TEXT);
        saved.setContent("Hi there!");

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(userRepository.findById("prov-1")).thenReturn(Optional.of(provider));

        MessageResponse result = conversationService.sendMessage("conv-1", request, userDetails);

        assertThat(result.getType()).isEqualTo(MessageType.TEXT);
        assertThat(result.getContent()).isEqualTo("Hi there!");
        verify(messagingTemplate).convertAndSendToUser(eq("provider@calpoly.edu"), eq("/queue/messages"), any());
        verify(notificationService).send(eq("prov-1"), any(), anyString(), anyString(), eq("conv-1"));
    }

    @Test
    void sendMessage_textFromProvider_pushesToCustomer() {
        SendMessageRequest request = new SendMessageRequest();
        request.setType(MessageType.TEXT);
        request.setContent("I can help with that.");

        Message saved = new Message();
        saved.setId("msg-provider");
        saved.setConversationId("conv-1");
        saved.setSenderId("prov-1");
        saved.setSenderName("Bob Smith");
        saved.setType(MessageType.TEXT);
        saved.setContent("I can help with that.");

        when(userDetails.getUsername()).thenReturn("provider@calpoly.edu");
        when(userRepository.findByEmail("provider@calpoly.edu")).thenReturn(Optional.of(provider));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(userRepository.findById("cust-1")).thenReturn(Optional.of(customer));

        MessageResponse result = conversationService.sendMessage("conv-1", request, userDetails);

        assertThat(result.getSenderId()).isEqualTo("prov-1");
        verify(messagingTemplate).convertAndSendToUser(eq("customer@calpoly.edu"), eq("/queue/messages"), any());
        verify(notificationService).send(eq("cust-1"), any(), anyString(), anyString(), eq("conv-1"));
    }

    @Test
    void sendMessage_emptyText_throwsIllegalArgumentException() {
        SendMessageRequest request = new SendMessageRequest();
        request.setType(MessageType.TEXT);
        request.setContent("   ");

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.sendMessage("conv-1", request, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    // --- sendMessage PRICE_OFFER ---

    @Test
    void sendMessage_priceOffer_savesAndNotifiesProvider() {
        SendMessageRequest request = new SendMessageRequest();
        request.setType(MessageType.PRICE_OFFER);
        request.setOfferedPrice(new BigDecimal("45.00"));

        Message saved = new Message();
        saved.setId("msg-3");
        saved.setConversationId("conv-1");
        saved.setSenderId("cust-1");
        saved.setType(MessageType.PRICE_OFFER);
        saved.setOfferedPrice(new BigDecimal("45.00"));

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(userRepository.findById("prov-1")).thenReturn(Optional.of(provider));

        MessageResponse result = conversationService.sendMessage("conv-1", request, userDetails);

        assertThat(result.getType()).isEqualTo(MessageType.PRICE_OFFER);
        assertThat(result.getOfferedPrice()).isEqualByComparingTo("45.00");
        verify(notificationService).send(eq("prov-1"), any(), anyString(), anyString(), eq("conv-1"));
    }

    @Test
    void sendMessage_priceOfferByProvider_throwsUnauthorizedChatAccessException() {
        SendMessageRequest request = new SendMessageRequest();
        request.setType(MessageType.PRICE_OFFER);
        request.setOfferedPrice(new BigDecimal("45.00"));

        when(userDetails.getUsername()).thenReturn("provider@calpoly.edu");
        when(userRepository.findByEmail("provider@calpoly.edu")).thenReturn(Optional.of(provider));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.sendMessage("conv-1", request, userDetails))
                .isInstanceOf(UnauthorizedChatAccessException.class)
                .hasMessageContaining("customer");
    }

    @Test
    void sendMessage_priceOfferNegativePrice_throwsIllegalArgumentException() {
        SendMessageRequest request = new SendMessageRequest();
        request.setType(MessageType.PRICE_OFFER);
        request.setOfferedPrice(new BigDecimal("-10.00"));

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.sendMessage("conv-1", request, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void sendMessage_acceptRejectTypeDirectly_throwsIllegalArgumentException() {
        SendMessageRequest request = new SendMessageRequest();
        request.setType(MessageType.PRICE_ACCEPTED);

        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.sendMessage("conv-1", request, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accept or reject endpoint");
    }

    // --- acceptOffer ---

    @Test
    void acceptOffer_validOffer_createsAcceptedMessageAndNotifiesCustomer() {
        when(userDetails.getUsername()).thenReturn("provider@calpoly.edu");

        Message offer = new Message();
        offer.setId("offer-1");
        offer.setConversationId("conv-1");
        offer.setSenderId("cust-1");
        offer.setType(MessageType.PRICE_OFFER);
        offer.setOfferedPrice(new BigDecimal("55.00"));
        offer.setOfferResponded(false);

        Message acceptedMsg = new Message();
        acceptedMsg.setId("msg-accepted");
        acceptedMsg.setConversationId("conv-1");
        acceptedMsg.setSenderId("prov-1");
        acceptedMsg.setType(MessageType.PRICE_ACCEPTED);
        acceptedMsg.setOfferedPrice(new BigDecimal("55.00"));
        acceptedMsg.setContent("Price offer of $55.00 accepted.");

        when(userRepository.findByEmail("provider@calpoly.edu")).thenReturn(Optional.of(provider));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageRepository.findById("offer-1")).thenReturn(Optional.of(offer));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            if (m.getType() == MessageType.PRICE_ACCEPTED) return acceptedMsg;
            return m;
        });
        when(conversationRepository.save(any())).thenReturn(conversation);
        when(userRepository.findById("cust-1")).thenReturn(Optional.of(customer));

        MessageResponse result = conversationService.acceptOffer("conv-1", "offer-1", userDetails);

        assertThat(result.getType()).isEqualTo(MessageType.PRICE_ACCEPTED);
        assertThat(offer.isOfferResponded()).isTrue();
        verify(messagingTemplate).convertAndSendToUser(eq("customer@calpoly.edu"), eq("/queue/messages"), any());
        verify(notificationService).send(eq("cust-1"), any(), anyString(), anyString(), eq("conv-1"));
    }

    @Test
    void acceptOffer_byCustomer_throwsUnauthorizedChatAccessException() {
        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.acceptOffer("conv-1", "offer-1", userDetails))
                .isInstanceOf(UnauthorizedChatAccessException.class)
                .hasMessageContaining("provider");
    }

    @Test
    void acceptOffer_alreadyResponded_throwsIllegalArgumentException() {
        when(userDetails.getUsername()).thenReturn("provider@calpoly.edu");

        Message offer = new Message();
        offer.setId("offer-1");
        offer.setConversationId("conv-1");
        offer.setType(MessageType.PRICE_OFFER);
        offer.setOfferResponded(true);

        when(userRepository.findByEmail("provider@calpoly.edu")).thenReturn(Optional.of(provider));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageRepository.findById("offer-1")).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> conversationService.acceptOffer("conv-1", "offer-1", userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already been responded");
    }

    @Test
    void acceptOffer_nonOfferMessage_throwsIllegalArgumentException() {
        when(userDetails.getUsername()).thenReturn("provider@calpoly.edu");

        Message textMsg = new Message();
        textMsg.setId("msg-text");
        textMsg.setConversationId("conv-1");
        textMsg.setType(MessageType.TEXT);

        when(userRepository.findByEmail("provider@calpoly.edu")).thenReturn(Optional.of(provider));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageRepository.findById("msg-text")).thenReturn(Optional.of(textMsg));

        assertThatThrownBy(() -> conversationService.acceptOffer("conv-1", "msg-text", userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a price offer");
    }

    @Test
    void acceptOffer_offerFromDifferentConversation_throwsUnauthorizedChatAccessException() {
        when(userDetails.getUsername()).thenReturn("provider@calpoly.edu");

        Message offer = new Message();
        offer.setId("offer-other");
        offer.setConversationId("other-conv");
        offer.setType(MessageType.PRICE_OFFER);
        offer.setOfferedPrice(new BigDecimal("55.00"));

        when(userRepository.findByEmail("provider@calpoly.edu")).thenReturn(Optional.of(provider));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageRepository.findById("offer-other")).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> conversationService.acceptOffer("conv-1", "offer-other", userDetails))
                .isInstanceOf(UnauthorizedChatAccessException.class)
                .hasMessageContaining("does not belong");
    }

    // --- rejectOffer ---

    @Test
    void rejectOffer_validOffer_createsRejectedMessageAndNotifiesCustomer() {
        when(userDetails.getUsername()).thenReturn("provider@calpoly.edu");

        Message offer = new Message();
        offer.setId("offer-2");
        offer.setConversationId("conv-1");
        offer.setSenderId("cust-1");
        offer.setType(MessageType.PRICE_OFFER);
        offer.setOfferedPrice(new BigDecimal("30.00"));
        offer.setOfferResponded(false);

        Message rejectedMsg = new Message();
        rejectedMsg.setId("msg-rejected");
        rejectedMsg.setConversationId("conv-1");
        rejectedMsg.setSenderId("prov-1");
        rejectedMsg.setType(MessageType.PRICE_REJECTED);
        rejectedMsg.setOfferedPrice(new BigDecimal("30.00"));

        when(userRepository.findByEmail("provider@calpoly.edu")).thenReturn(Optional.of(provider));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageRepository.findById("offer-2")).thenReturn(Optional.of(offer));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            if (m.getType() == MessageType.PRICE_REJECTED) return rejectedMsg;
            return m;
        });
        when(conversationRepository.save(any())).thenReturn(conversation);
        when(userRepository.findById("cust-1")).thenReturn(Optional.of(customer));

        MessageResponse result = conversationService.rejectOffer("conv-1", "offer-2", userDetails);

        assertThat(result.getType()).isEqualTo(MessageType.PRICE_REJECTED);
        assertThat(offer.isOfferResponded()).isTrue();
        verify(notificationService).send(eq("cust-1"), any(), anyString(), anyString(), eq("conv-1"));
    }

    // --- getUnreadMessageCount ---

    @Test
    void getUnreadMessageCount_noConversations_returnsZero() {
        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findByCustomerIdOrProviderIdOrderByLastMessageAtDesc("cust-1", "cust-1"))
                .thenReturn(List.of());

        long count = conversationService.getUnreadMessageCount(userDetails);

        assertThat(count).isZero();
        verify(messageRepository, never()).countBySenderIdNotAndConversationIdInAndReadFalse(any(), any());
    }

    @Test
    void getUnreadMessageCount_withUnread_returnsCorrectTotal() {
        when(userRepository.findByEmail("customer@calpoly.edu")).thenReturn(Optional.of(customer));
        when(conversationRepository.findByCustomerIdOrProviderIdOrderByLastMessageAtDesc("cust-1", "cust-1"))
                .thenReturn(List.of(conversation));
        when(messageRepository.countBySenderIdNotAndConversationIdInAndReadFalse(eq("cust-1"), any()))
                .thenReturn(3L);

        long count = conversationService.getUnreadMessageCount(userDetails);

        assertThat(count).isEqualTo(3L);
    }
}
