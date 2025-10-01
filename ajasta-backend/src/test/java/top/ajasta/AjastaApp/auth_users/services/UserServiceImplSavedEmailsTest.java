package top.ajasta.AjastaApp.auth_users.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.auth_users.repository.UserRepository;
import top.ajasta.AjastaApp.aws.AWSS3Service;
import top.ajasta.AjastaApp.email_notification.services.NotificationService;
import top.ajasta.AjastaApp.exceptions.BadRequestException;
import top.ajasta.AjastaApp.response.Response;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.ajasta.AjastaApp.role.repository.RoleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class UserServiceImplSavedEmailsTest {

    private UserRepository userRepository;
    private UserServiceImpl service;

    private final String currentEmail = "john@example.com";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ModelMapper modelMapper = new ModelMapper();
        NotificationService notificationService = mock(NotificationService.class);
        AWSS3Service awss3Service = mock(AWSS3Service.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        service = new UserServiceImpl(userRepository, passwordEncoder, modelMapper, notificationService, awss3Service, roleRepository);

        // Security context with current user email as principal
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(currentEmail, null));

        // Default user returned by repository
        User u = new User();
        u.setEmail(currentEmail);
        when(userRepository.findByEmail(eq(currentEmail))).thenReturn(Optional.of(u));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSavedEmails_initiallyEmpty() {
        User u = userRepository.findByEmail(currentEmail).orElseThrow();
        u.setSavedEmails(null);

        Response<List<String>> resp = service.getSavedEmails();
        assertEquals(200, resp.getStatusCode());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    @Test
    void addSavedEmail_addsNormalized_andPersists() {
        User u = userRepository.findByEmail(currentEmail).orElseThrow();
        u.setSavedEmails(new ArrayList<>());

        Response<?> resp = service.addSavedEmail("Friend@Example.com");
        assertEquals(200, resp.getStatusCode());
        assertEquals("Saved", resp.getMessage());

        // Verify email normalized to lowercase and saved
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(cap.capture());
        List<String> emails = cap.getValue().getSavedEmails();
        assertNotNull(emails);
        assertEquals(1, emails.size());
        assertEquals("friend@example.com", emails.getFirst());
    }

    @Test
    void addSavedEmail_rejectsInvalidFormat() {
        User u = userRepository.findByEmail(currentEmail).orElseThrow();
        u.setSavedEmails(new ArrayList<>());

        assertThrows(BadRequestException.class, () -> service.addSavedEmail("bad-email"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void addSavedEmail_duplicateDoesNotSaveTwice() {
        User u = userRepository.findByEmail(currentEmail).orElseThrow();
        ArrayList<String> list = new ArrayList<>();
        list.add("friend@example.com");
        u.setSavedEmails(list);

        // Attempt to add duplicate
        Response<?> resp = service.addSavedEmail("friend@example.com");
        assertEquals(200, resp.getStatusCode());
        // No additional save
        verify(userRepository, never()).save(any());
    }
}
