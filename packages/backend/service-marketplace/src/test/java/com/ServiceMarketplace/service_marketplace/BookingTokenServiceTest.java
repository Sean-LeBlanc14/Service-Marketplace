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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ServiceMarketplace.service_marketplace.exception.BookingTokenException;
import com.ServiceMarketplace.service_marketplace.model.BookingToken;
import com.ServiceMarketplace.service_marketplace.model.BookingTokenAction;
import com.ServiceMarketplace.service_marketplace.repository.BookingTokenRepository;
import com.ServiceMarketplace.service_marketplace.service.BookingTokenService;

@ExtendWith(MockitoExtension.class)
class BookingTokenServiceTest {

    @Mock
    private BookingTokenRepository bookingTokenRepository;

    @InjectMocks
    private BookingTokenService bookingTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingTokenService, "appBaseUrl", "https://polyservices.test");
    }

    @Test
    void generateTokenPair_savesConfirmAndCancelTokensAndReturnsUrls() {
        when(bookingTokenRepository.save(any(BookingToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingTokenService.TokenPair tokenPair = bookingTokenService.generateTokenPair("booking-123");

        ArgumentCaptor<BookingToken> tokenCaptor = ArgumentCaptor.forClass(BookingToken.class);
        verify(bookingTokenRepository, org.mockito.Mockito.times(2)).save(tokenCaptor.capture());

        List<BookingToken> savedTokens = tokenCaptor.getAllValues();
        assertThat(savedTokens).hasSize(2);
        assertThat(savedTokens.get(0).getBookingId()).isEqualTo("booking-123");
        assertThat(savedTokens.get(0).getAction()).isEqualTo(BookingTokenAction.CONFIRM);
        assertThat(savedTokens.get(0).getExpiresAt()).isAfter(Instant.now());
        assertThat(savedTokens.get(1).getBookingId()).isEqualTo("booking-123");
        assertThat(savedTokens.get(1).getAction()).isEqualTo(BookingTokenAction.CANCEL);
        assertThat(savedTokens.get(1).getExpiresAt()).isAfter(Instant.now());
        assertThat(savedTokens.get(0).getToken()).isNotBlank();
        assertThat(savedTokens.get(1).getToken()).isNotBlank();
        assertThat(savedTokens.get(0).getToken()).isNotEqualTo(savedTokens.get(1).getToken());
        assertThat(tokenPair.confirmUrl())
            .isEqualTo("https://polyservices.test/api/bookings/action?token=" + savedTokens.get(0).getToken());
        assertThat(tokenPair.cancelUrl())
            .isEqualTo("https://polyservices.test/api/bookings/action?token=" + savedTokens.get(1).getToken());
    }

    @Test
    void validateAndConsume_validToken_marksTokenUsedAndReturnsResult() {
        BookingToken token = new BookingToken();
        token.setToken("confirm-token");
        token.setBookingId("booking-123");
        token.setAction(BookingTokenAction.CONFIRM);
        token.setExpiresAt(Instant.now().plusSeconds(60));

        when(bookingTokenRepository.findByToken("confirm-token")).thenReturn(Optional.of(token));

        BookingTokenService.TokenResult result = bookingTokenService.validateAndConsume("confirm-token");

        assertThat(result.bookingId()).isEqualTo("booking-123");
        assertThat(result.action()).isEqualTo(BookingTokenAction.CONFIRM);
        assertThat(token.isUsed()).isTrue();
        verify(bookingTokenRepository).save(token);
    }

    @Test
    void validateAndConsume_missingToken_throwsBookingTokenException() {
        when(bookingTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingTokenService.validateAndConsume("missing-token"))
            .isInstanceOf(BookingTokenException.class)
            .hasMessage("Invalid or unrecognized confirmation link");

        verify(bookingTokenRepository, never()).save(any(BookingToken.class));
    }

    @Test
    void validateAndConsume_usedToken_throwsBookingTokenException() {
        BookingToken token = new BookingToken();
        token.setToken("used-token");
        token.setUsed(true);
        token.setExpiresAt(Instant.now().plusSeconds(60));

        when(bookingTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> bookingTokenService.validateAndConsume("used-token"))
            .isInstanceOf(BookingTokenException.class)
            .hasMessage("This link has already been used");

        verify(bookingTokenRepository, never()).save(any(BookingToken.class));
    }

    @Test
    void validateAndConsume_expiredToken_throwsBookingTokenException() {
        BookingToken token = new BookingToken();
        token.setToken("expired-token");
        token.setExpiresAt(Instant.now().minusSeconds(60));

        when(bookingTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> bookingTokenService.validateAndConsume("expired-token"))
            .isInstanceOf(BookingTokenException.class)
            .hasMessage("This confirmation link has expired");

        verify(bookingTokenRepository, never()).save(any(BookingToken.class));
    }
}
