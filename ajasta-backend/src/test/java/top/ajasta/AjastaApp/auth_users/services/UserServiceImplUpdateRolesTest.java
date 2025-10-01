package top.ajasta.AjastaApp.auth_users.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.ajasta.AjastaApp.auth_users.dtos.UserDTO;
import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.auth_users.repository.UserRepository;
import top.ajasta.AjastaApp.aws.AWSS3Service;
import top.ajasta.AjastaApp.email_notification.services.NotificationService;
import top.ajasta.AjastaApp.exceptions.BadRequestException;
import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.role.entity.Role;
import top.ajasta.AjastaApp.role.repository.RoleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceImplUpdateRolesTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserServiceImpl service;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ModelMapper modelMapper = new ModelMapper();
        NotificationService notificationService = mock(NotificationService.class);
        AWSS3Service awss3Service = mock(AWSS3Service.class);
        service = new UserServiceImpl(userRepository, passwordEncoder, modelMapper, notificationService, awss3Service, roleRepository);

        // mock security principal
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin@ajastaapp.com", null));
    }

    @Test
    void updateUserRoles_normalizesAndUpdates() {
        // given
        User user = new User();
        user.setId(8L);
        user.setEmail("user8@example.com");
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));

        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(Role.builder().id(1L).name("ADMIN").build()));
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(Role.builder().id(2L).name("CUSTOMER").build()));

        // when
        Response<UserDTO> resp = service.updateUserRoles(8L, List.of("ROLE_ADMIN", "customer", " ADMIN "));

        // then
        assertEquals(200, resp.getStatusCode());
        assertNotNull(resp.getData());
        assertNotNull(resp.getData().getRoles());
        assertEquals(2, resp.getData().getRoles().size());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUserRoles_unknownRole_throwsBadRequest() {
        // given
        User user = new User();
        user.setId(9L);
        user.setEmail("user9@example.com");
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        // then
        assertThrows(BadRequestException.class, () -> service.updateUserRoles(9L, List.of("UNKNOWN")));
        verify(userRepository, never()).save(any());
    }
}
