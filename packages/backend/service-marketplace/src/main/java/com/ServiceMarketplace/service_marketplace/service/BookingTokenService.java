package com.ServiceMarketplace.service_marketplace.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.exception.BookingTokenException;
import com.ServiceMarketplace.service_marketplace.model.BookingToken;
import com.ServiceMarketplace.service_marketplace.model.BookingTokenAction;
import com.ServiceMarketplace.service_marketplace.repository.BookingTokenRepository;

@Service
public class BookingTokenService {

    private static final int TOKEN_EXPIRY_HOURS = 48;
    private static final int TOKEN_BYTES = 32;

    @Value("${custom.app.base-url}")
    private String appBaseUrl;

    private final BookingTokenRepository bookingTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public BookingTokenService(BookingTokenRepository bookingTokenRepository) {
        this.bookingTokenRepository = bookingTokenRepository;
    }

    public record TokenPair(String confirmUrl, String cancelUrl) {}

    public record TokenResult(String bookingId, BookingTokenAction action) {}

    public TokenPair generateTokenPair(String bookingId) {
        String confirmToken = generateToken();
        String cancelToken = generateToken();

        bookingTokenRepository.save(buildToken(confirmToken, bookingId, BookingTokenAction.CONFIRM));
        bookingTokenRepository.save(buildToken(cancelToken, bookingId, BookingTokenAction.CANCEL));

        return new TokenPair(
            appBaseUrl + "/api/bookings/action?token=" + confirmToken,
            appBaseUrl + "/api/bookings/action?token=" + cancelToken
        );
    }

    public TokenResult validateAndConsume(String token) {
        BookingToken bookingToken = bookingTokenRepository.findByToken(token)
            .orElseThrow(() -> new BookingTokenException("Invalid or unrecognized confirmation link"));

        if (bookingToken.isUsed()) {
            throw new BookingTokenException("This link has already been used");
        }

        if (Instant.now().isAfter(bookingToken.getExpiresAt())) {
            throw new BookingTokenException("This confirmation link has expired");
        }

        bookingToken.setUsed(true);
        bookingTokenRepository.save(bookingToken);

        return new TokenResult(bookingToken.getBookingId(), bookingToken.getAction());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private BookingToken buildToken(String token, String bookingId, BookingTokenAction action) {
        BookingToken bt = new BookingToken();
        bt.setToken(token);
        bt.setBookingId(bookingId);
        bt.setAction(action);
        bt.setExpiresAt(Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS));
        return bt;
    }
}
