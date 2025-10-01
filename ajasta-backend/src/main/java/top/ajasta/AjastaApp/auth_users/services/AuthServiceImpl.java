package top.ajasta.AjastaApp.auth_users.services;


import top.ajasta.AjastaApp.auth_users.dtos.LoginRequest;
import top.ajasta.AjastaApp.auth_users.dtos.LoginResponse;
import top.ajasta.AjastaApp.auth_users.dtos.RegistrationRequest;
import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.auth_users.repository.UserRepository;
import top.ajasta.AjastaApp.exceptions.BadRequestException;
import top.ajasta.AjastaApp.exceptions.NotFoundException;
import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.role.entity.Role;
import top.ajasta.AjastaApp.role.repository.RoleRepository;
import top.ajasta.AjastaApp.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RoleRepository roleRepository;
    private final jakarta.servlet.http.HttpServletRequest request;
    private final jakarta.servlet.http.HttpServletResponse response;


    @Override
    public Response<?> register(RegistrationRequest registrationRequest) {

        log.info("INSIDE register()");

        // Validate the registration request
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        // collect all roles from the request
        List<Role> userRoles;
        if (registrationRequest.getRoles() != null && !registrationRequest.getRoles().isEmpty()) {
            userRoles = registrationRequest.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(roleName.toUpperCase())
                            .orElseThrow(() -> new NotFoundException("Role '" + roleName + "' Not Found")))
                    .toList();
        } else {
            // If no roles provided, default to CUSTOMER
            Role defaultRole = roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new NotFoundException("Default CUSTOMER role Not Found"));
            userRoles = List.of(defaultRole);
        }
        // Build the user object
        User userToSave = User.builder()
                .name(registrationRequest.getName())
                .email(registrationRequest.getEmail())
                .phoneNumber(registrationRequest.getPhoneNumber())
                .address(registrationRequest.getAddress())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .roles(userRoles)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Save the user
        userRepository.save(userToSave);

        log.info("User registered successfully");

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("User Registered Successfully")
                .build();


    }

    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest) {

        log.info("INSIDE login()");

        // Find the user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid Email"));

        if (!user.isActive()) {
            throw new NotFoundException("Account not active, Please contact customer support");
        }

        // Verify the password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid Password");
        }

        // Generate a token bound to current User-Agent and a per-session cookie to prevent token pasting
        String ua = request != null ? request.getHeader("User-Agent") : null;
        String uaHash = jwtUtils.hashUserAgent(ua);
        String sessionId = UUID.randomUUID().toString();

        // Issue HttpOnly session cookie (AJASTA_SID) used to bind JWT to this browser session
        try {
            ResponseCookie sidCookie = ResponseCookie.from("AJASTA_SID", sessionId)
                    .httpOnly(true)
                    .secure(false) // set to true when serving over HTTPS
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(30L * 24 * 60 * 60) // 30 days in seconds (align with JWT)
                    .build();
            if (response != null) {
                response.addHeader("Set-Cookie", sidCookie.toString());
            }
        } catch (Exception ignored) {}

        String token = jwtUtils.generateToken(user.getEmail(), uaHash, sessionId);

        // Extract role names as a list
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);
        loginResponse.setRoles(roleNames);

        return Response.<LoginResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Login Successful")
                .data(loginResponse)
                .build();
    }
}








