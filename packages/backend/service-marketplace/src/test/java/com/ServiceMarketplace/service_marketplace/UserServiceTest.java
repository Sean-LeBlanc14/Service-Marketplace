package com.ServiceMarketplace.service_marketplace;

import com.ServiceMarketplace.service_marketplace.dto.AuthResponse;
import com.ServiceMarketplace.service_marketplace.dto.ChangePasswordRequest;
import com.ServiceMarketplace.service_marketplace.dto.RegisterRequest;
import com.ServiceMarketplace.service_marketplace.dto.UserProfile;
import com.ServiceMarketplace.service_marketplace.exception.EmailAlreadyExistsException;
import com.ServiceMarketplace.service_marketplace.exception.RedundantChangeException;
import com.ServiceMarketplace.service_marketplace.exception.InvalidEmailDomainException;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.EmailService;
import com.ServiceMarketplace.service_marketplace.service.JwtService;
import com.ServiceMarketplace.service_marketplace.service.ServiceService;
import com.ServiceMarketplace.service_marketplace.service.UserService;
import com.ServiceMarketplace.service_marketplace.service.VerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationService verificationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private ServiceService serviceService;

    @InjectMocks
    private UserService userService;

    private User createProfileUser() {
        User user = new User();
        user.setId("user123");
        user.setEmail("student@example.com");
        user.setPassword("encoded-old-password");
        user.setFirstName("Avery");
        user.setLastName("Chen");
        user.setMajor("Computer Science");
        user.setCampus("Pomona");
        return user;
    }

    private UserDetails mockUserDetails() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("student@example.com");
        return userDetails;
    }

    @Test
    void registerUser_success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@calpoly.edu");
        request.setPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedpassword");
        when(jwtService.generateToken(request.getEmail())).thenReturn("jwt-token");
        when(verificationService.generateVerificationCode()).thenReturn("123456");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId("abc123");
            return u;
        });

        AuthResponse response = userService.registerUser(request);

        assertEquals("test@calpoly.edu", response.getEmail());
        assertEquals("abc123", response.getId());
        assertEquals("jwt-token", response.getToken());
        verify(verificationService).createVerification(request.getEmail(), "123456");
        verify(emailService).sendVerificationEmail(request.getEmail(), "123456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@calpoly.edu");
        request.setPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserPassword_success_updatesEncodedPassword() {
        User user = createProfileUser();
        UserDetails userDetails = mockUserDetails();
        ChangePasswordRequest request = new ChangePasswordRequest("old-password", "new-password");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "encoded-old-password")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");

        String result = userService.changeUserPassword(userDetails, request);

        assertEquals("Success", result);
        assertEquals("encoded-new-password", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changeUserPassword_invalidCurrentPassword_throwsBadCredentials() {
        User user = createProfileUser();
        UserDetails userDetails = mockUserDetails();
        ChangePasswordRequest request = new ChangePasswordRequest("wrong-password", "new-password");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-old-password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userService.changeUserPassword(userDetails, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserPassword_sameAsCurrentPassword_throwsRedundantChange() {
        User user = createProfileUser();
        UserDetails userDetails = mockUserDetails();
        ChangePasswordRequest request = new ChangePasswordRequest("old-password", "old-password");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "encoded-old-password")).thenReturn(true);

        assertThrows(RedundantChangeException.class, () -> userService.changeUserPassword(userDetails, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserPassword_missingUser_throwsUsernameNotFound() {
        UserDetails userDetails = mockUserDetails();
        ChangePasswordRequest request = new ChangePasswordRequest("old-password", "new-password");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.changeUserPassword(userDetails, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserMajor_success_updatesMajorAndReturnsProfile() {
        User user = createProfileUser();
        UserDetails userDetails = mockUserDetails();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(serviceService.getServicesByUserId("user123")).thenReturn(List.of());

        UserProfile result = userService.changeUserMajor(userDetails, "Electrical Engineering");

        assertEquals("Electrical Engineering", user.getMajor());
        assertEquals("Electrical Engineering", result.getMajor());
        assertEquals("student@example.com", result.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void changeUserMajor_sameMajor_throwsRedundantChange() {
        User user = createProfileUser();
        UserDetails userDetails = mockUserDetails();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        assertThrows(RedundantChangeException.class, () -> userService.changeUserMajor(userDetails, "Computer Science"));
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(serviceService);
    }

    @Test
    void changeUserMajor_missingUser_throwsUsernameNotFound() {
        UserDetails userDetails = mockUserDetails();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.changeUserMajor(userDetails, "Business"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserCampus_success_updatesCampusAndReturnsProfile() {
        User user = createProfileUser();
        UserDetails userDetails = mockUserDetails();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(serviceService.getServicesByUserId("user123")).thenReturn(List.of());

        UserProfile result = userService.changeUserCampus(userDetails, "San Luis Obispo");

        assertEquals("San Luis Obispo", user.getCampus());
        assertEquals("San Luis Obispo", result.getCampus());
        assertEquals("student@example.com", result.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void changeUserCampus_sameCampus_throwsRedundantChange() {
        User user = createProfileUser();
        UserDetails userDetails = mockUserDetails();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        assertThrows(RedundantChangeException.class, () -> userService.changeUserCampus(userDetails, "Pomona"));
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(serviceService);
    }

    @Test
    void changeUserCampus_missingUser_throwsUsernameNotFound() {
        UserDetails userDetails = mockUserDetails();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.changeUserCampus(userDetails, "San Luis Obispo"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_nonCalPolyEmail_throwsInvalidEmailDomainException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password123");

        assertThrows(InvalidEmailDomainException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).existsByEmail(any());
    }
}
