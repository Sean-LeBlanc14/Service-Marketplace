package com.ServiceMarketplace.service_marketplace;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ServiceMarketplace.service_marketplace.dto.AuthResponse;
import com.ServiceMarketplace.service_marketplace.dto.ChangePasswordRequest;
import com.ServiceMarketplace.service_marketplace.dto.RegisterRequest;
import com.ServiceMarketplace.service_marketplace.dto.DeleteAccountRequest;
import com.ServiceMarketplace.service_marketplace.dto.LoginRequest;
import com.ServiceMarketplace.service_marketplace.dto.ProviderProfile;
import com.ServiceMarketplace.service_marketplace.dto.UpdateUserProfileRequest;
import com.ServiceMarketplace.service_marketplace.dto.UserProfile;
import com.ServiceMarketplace.service_marketplace.exception.EmailAlreadyExistsException;
import com.ServiceMarketplace.service_marketplace.exception.FailedToDeleteUserException;
import com.ServiceMarketplace.service_marketplace.exception.InvalidEmailDomainException;
import com.ServiceMarketplace.service_marketplace.exception.RedundantChangeException;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.EmailService;
import com.ServiceMarketplace.service_marketplace.service.JwtService;
import com.ServiceMarketplace.service_marketplace.service.ServiceService;
import com.ServiceMarketplace.service_marketplace.service.UserService;
import com.ServiceMarketplace.service_marketplace.service.VerificationService;

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

        userService.changeUserPassword(userDetails, request);

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
    void getProviderProfile_suspendedUser_throwsResourceNotFound() {
        User user = createProfileUser();
        user.setRole("suspended");

        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () -> userService.getProviderProfile("user123"));
        verifyNoInteractions(serviceService);
    }

    @Test
    void changeUserMajor_success_updatesMajorAndReturnsProfile() {
        User user = createProfileUser();
        UserDetails userDetails = mockUserDetails();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(serviceService.getServicesByUserId("user123")).thenReturn(List.of());
        when(serviceService.getProviderRatingSummary("user123")).thenReturn(new ServiceService.ProviderRatingSummary(null, 0));

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
        when(serviceService.getProviderRatingSummary("user123")).thenReturn(new ServiceService.ProviderRatingSummary(null, 0));

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

    @Test
    void loginUser_success_authenticatesAndReturnsAuthResponse() {
        LoginRequest request = new LoginRequest("student@example.com", "password123");
        User user = createProfileUser();
        user.setRole("admin");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("student@example.com")).thenReturn("jwt-token");

        AuthResponse response = userService.loginUser(request);

        assertEquals("user123", response.getId());
        assertEquals("student@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
        assertEquals("admin", response.getRole());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void loginUser_badCredentials_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest("student@example.com", "wrongpass");

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> userService.loginUser(request));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void getUserProfile_success_returnsProfileWithServicesAndRating() {
        User user = createProfileUser();
        user.setBio("  Tutor and builder  ");
        user.setVerificationStatus(true);
        UserDetails userDetails = mockUserDetails();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(serviceService.getProviderRatingSummary("user123"))
            .thenReturn(new ServiceService.ProviderRatingSummary(4.5, 2));
        when(serviceService.getServicesByUserId("user123")).thenReturn(List.of());

        UserProfile profile = userService.getUserProfile(userDetails);

        assertEquals("Tutor and builder", profile.getBio());
        assertEquals(true, profile.isVerified());
        assertEquals(4.5, profile.getAverageRating());
        assertEquals(2, profile.getReviewCount());
    }

    @Test
    void getProviderProfile_success_returnsProviderProfile() {
        User user = createProfileUser();
        user.setBio("  I tutor math  ");

        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(serviceService.getProviderRatingSummary("user123"))
            .thenReturn(new ServiceService.ProviderRatingSummary(5.0, 3));
        when(serviceService.getServicesByUserId("user123")).thenReturn(List.of());

        ProviderProfile profile = userService.getProviderProfile("user123");

        assertEquals("user123", profile.getId());
        assertEquals("I tutor math", profile.getBio());
        assertEquals(5.0, profile.getAverageRating());
        assertEquals(3, profile.getReviewCount());
    }

    @Test
    void deleteUserProfile_success_deletesAuthenticatedUser() {
        DeleteAccountRequest request = new DeleteAccountRequest("student@example.com", "password123");
        User user = createProfileUser();

        when(userRepository.deleteByEmail("student@example.com")).thenReturn(Optional.of(user));

        userService.deleteUserProfile(request);

        verify(authenticationManager).authenticate(any());
        verify(userRepository).deleteByEmail("student@example.com");
    }

    @Test
    void deleteUserProfile_badCredentials_throwsBadCredentialsException() {
        DeleteAccountRequest request = new DeleteAccountRequest("student@example.com", "wrongpass");

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> userService.deleteUserProfile(request));
        verify(userRepository, never()).deleteByEmail(any());
    }

    @Test
    void deleteUserProfile_missingDeleteResult_throwsFailedToDeleteUserException() {
        DeleteAccountRequest request = new DeleteAccountRequest("student@example.com", "password123");

        when(userRepository.deleteByEmail("student@example.com")).thenReturn(Optional.empty());

        assertThrows(FailedToDeleteUserException.class, () -> userService.deleteUserProfile(request));
    }

    @Test
    void updateUserProfile_success_updatesBioAndReturnsProfile() {
        User user = createProfileUser();
        UserDetails userDetails = mockUserDetails();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setBio("  Updated bio  ");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(serviceService.getProviderRatingSummary("user123"))
            .thenReturn(new ServiceService.ProviderRatingSummary(null, 0));
        when(serviceService.getServicesByUserId("user123")).thenReturn(List.of());

        UserProfile profile = userService.updateUserProfile(userDetails, request);

        assertEquals("Updated bio", user.getBio());
        assertEquals("Updated bio", profile.getBio());
    }

    @Test
    void getUserByEmail_success_returnsUser() {
        User user = createProfileUser();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        assertEquals(user, userService.getUserByEmail("student@example.com"));
    }

    @Test
    void getUserByEmail_missing_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.getUserByEmail("missing@example.com"));
    }

    @Test
    void isAdmin_adminUser_returnsTrue() {
        User user = createProfileUser();
        user.setRole("admin");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        assertEquals(true, userService.isAdmin("admin@example.com"));
    }

    @Test
    void getAllUsers_returnsRepositoryUsers() {
        User user = createProfileUser();
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertEquals(List.of(user), userService.getAllUsers());
    }

    @Test
    void suspendUser_regularUser_setsSuspendedRole() {
        User user = createProfileUser();
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.suspendUser("user123");

        assertEquals("suspended", result.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void suspendUser_admin_throwsAccessDeniedException() {
        User user = createProfileUser();
        user.setRole("admin");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, () -> userService.suspendUser("user123"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void unsuspendUser_suspendedUser_setsUserRole() {
        User user = createProfileUser();
        user.setRole("suspended");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.unsuspendUser("user123");

        assertEquals("user", result.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void unsuspendUser_admin_throwsAccessDeniedException() {
        User user = createProfileUser();
        user.setRole("admin");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, () -> userService.unsuspendUser("user123"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void unsuspendUser_regularUser_returnsUserWithoutSaving() {
        User user = createProfileUser();
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        User result = userService.unsuspendUser("user123");

        assertEquals(user, result);
        verify(userRepository, never()).save(any(User.class));
    }
}
