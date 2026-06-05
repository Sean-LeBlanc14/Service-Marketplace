package com.ServiceMarketplace.service_marketplace;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ServiceMarketplace.service_marketplace.dto.ResendResponse;
import com.ServiceMarketplace.service_marketplace.dto.VerificationRequest;
import com.ServiceMarketplace.service_marketplace.dto.VerifiedResponse;
import com.ServiceMarketplace.service_marketplace.exception.InvalidVerificationCode;
import com.ServiceMarketplace.service_marketplace.exception.VerificationCodeExpired;
import com.ServiceMarketplace.service_marketplace.exception.VerificationNotFound;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.model.Verification;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.repository.VerificationRepository;
import com.ServiceMarketplace.service_marketplace.service.VerificationService;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private VerificationService verificationService;

    @Test
    void createVerification_savesEncodedVerificationCode() {
        when(passwordEncoder.encode("123456")).thenReturn("encoded-code");

        verificationService.createVerification("student@calpoly.edu", "123456");

        ArgumentCaptor<Verification> savedVerification = ArgumentCaptor.forClass(Verification.class);
        verify(verificationRepository).save(savedVerification.capture());
        assertThat(savedVerification.getValue().getEmail()).isEqualTo("student@calpoly.edu");
        assertThat(savedVerification.getValue().getVerificationCode()).isEqualTo("encoded-code");
        assertThat(savedVerification.getValue().getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    void verifyCode_validCode_marksUserVerifiedAndDeletesVerification() {
        Verification verification = new Verification("student@calpoly.edu", "encoded-code");
        User user = new User();
        user.setEmail("student@calpoly.edu");

        when(verificationRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true);
        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(user));

        VerifiedResponse response = verificationService.verifyCode(
            new VerificationRequest("123456"),
            "student@calpoly.edu"
        );

        assertThat(response.verified()).isTrue();
        assertThat(response.message()).isEqualTo("Account verified successfully. You can now log in.");
        assertThat(user.getVerificationStatus()).isTrue();
        verify(userRepository).save(user);
        verify(verificationRepository).delete(verification);
    }

    @Test
    void verifyCode_missingVerification_throwsVerificationNotFound() {
        when(verificationRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.verifyCode(
            new VerificationRequest("123456"),
            "student@calpoly.edu"
        )).isInstanceOf(VerificationNotFound.class);

        verifyNoInteractions(passwordEncoder, userRepository);
    }

    @Test
    void verifyCode_expiredVerification_throwsVerificationCodeExpired() {
        Verification verification = new Verification("student@calpoly.edu", "encoded-code");
        verification.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        when(verificationRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> verificationService.verifyCode(
            new VerificationRequest("123456"),
            "student@calpoly.edu"
        )).isInstanceOf(VerificationCodeExpired.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verifyNoInteractions(userRepository);
    }

    @Test
    void verifyCode_invalidCode_throwsInvalidVerificationCode() {
        Verification verification = new Verification("student@calpoly.edu", "encoded-code");

        when(verificationRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("000000", "encoded-code")).thenReturn(false);

        assertThatThrownBy(() -> verificationService.verifyCode(
            new VerificationRequest("000000"),
            "student@calpoly.edu"
        )).isInstanceOf(InvalidVerificationCode.class);

        verifyNoInteractions(userRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyCode_missingUser_throwsUsernameNotFoundException() {
        Verification verification = new Verification("student@calpoly.edu", "encoded-code");

        when(verificationRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true);
        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.verifyCode(
            new VerificationRequest("123456"),
            "student@calpoly.edu"
        )).isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(verificationRepository, never()).delete(any(Verification.class));
    }

    @Test
    void resendCode_updatesStoredCodeAndExpiryDate() {
        Verification verification = new Verification("student@calpoly.edu", "old-code");
        LocalDateTime oldExpiry = LocalDateTime.now().minusMinutes(1);
        verification.setExpiryDate(oldExpiry);

        when(verificationRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(verification));
        when(passwordEncoder.encode(anyString())).thenReturn("new-encoded-code");

        ResendResponse response = verificationService.resendCode(
            new VerificationRequest("000000"),
            "student@calpoly.edu"
        );

        assertThat(response.getEmail()).isEqualTo("student@calpoly.edu");
        assertThat(response.getCode()).matches("\\d{6}");
        assertThat(verification.getVerificationCode()).isEqualTo("new-encoded-code");
        assertThat(verification.getExpiryDate()).isAfter(oldExpiry);
        verify(passwordEncoder).encode(response.getCode());
        verify(verificationRepository).save(verification);
    }

    @Test
    void resendCode_missingVerification_throwsVerificationNotFound() {
        when(verificationRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.resendCode(
            new VerificationRequest("000000"),
            "student@calpoly.edu"
        )).isInstanceOf(VerificationNotFound.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(verificationRepository, never()).save(any(Verification.class));
    }

    @Test
    void generateVerificationCode_returnsSixDigits() {
        assertThat(verificationService.generateVerificationCode()).matches("\\d{6}");
    }
}
