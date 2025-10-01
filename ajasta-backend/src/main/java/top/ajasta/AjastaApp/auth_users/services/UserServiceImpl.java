package top.ajasta.AjastaApp.auth_users.services;


import top.ajasta.AjastaApp.auth_users.dtos.UserDTO;
import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.auth_users.repository.UserRepository;
import top.ajasta.AjastaApp.aws.AWSS3Service;
import top.ajasta.AjastaApp.email_notification.dtos.NotificationDTO;
import top.ajasta.AjastaApp.email_notification.services.NotificationService;
import top.ajasta.AjastaApp.exceptions.BadRequestException;
import top.ajasta.AjastaApp.exceptions.NotFoundException;
import top.ajasta.AjastaApp.response.Response;
import top.ajasta.AjastaApp.role.entity.Role;
import top.ajasta.AjastaApp.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;
    private final AWSS3Service awss3Service;
    private final RoleRepository roleRepository;


    @Override
    public User getCurrentLoggedInUser() {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(()-> new NotFoundException("user not found"));

    }

    @Override
    public Response<List<UserDTO>> getAllUsers() {

        log.info("INSIDE getAllUsers()");

        List<User> userList = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        List<UserDTO> userDTOS = modelMapper.map(userList, new TypeToken<List<UserDTO>>() {
        }.getType());

        return Response.<List<UserDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("All users retreived successfully")
                .data(userDTOS)
                .build();
    }

    @Override
    public Response<UserDTO> getOwnAccountDetails() {

        log.info("INSIDE getOwnAccountDetails()");

        User user = getCurrentLoggedInUser();

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        return Response.<UserDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("success")
                .data(userDTO)
                .build();

    }

    @Override
    public Response<?> updateOwnAccount(UserDTO userDTO) {

        log.info("INSIDE updateOwnAccount()");

        // Fetch the currently logged-in user
        User user = getCurrentLoggedInUser();

        String profileUrl = user.getProfileUrl();
        MultipartFile imageFile = userDTO.getImageFile();


        log.info("EXISTIN Profile URL IS: " + profileUrl);

        // Check if a new imageFile was provided
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete the old image from S3 if it exists
            if (profileUrl != null && !profileUrl.isEmpty()) {
                String keyName = profileUrl.substring(profileUrl.lastIndexOf("/") + 1);
                awss3Service.deleteFile("profile/" + keyName);

                log.info("Deleted old profile image from s3");
            }
            //upload new image
            String imageName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            URL newImageUrl = awss3Service.uploadFile("profile/" + imageName, imageFile);

            user.setProfileUrl(newImageUrl.toString());
        }


        // Update user details
        if (userDTO.getName() != null) {
            user.setName(userDTO.getName());
        }

        if (userDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(userDTO.getPhoneNumber());
        }

        if (userDTO.getAddress() != null) {
            user.setAddress(userDTO.getAddress());
        }

        if (userDTO.getEmail() != null && !userDTO.getEmail().equals(user.getEmail())) {
            // Check if the new email is already taken
            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(userDTO.getEmail());
        }

        if (userDTO.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        // Save the updated user
        userRepository.save(user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account updated successfully")
                .build();

    }

    @Override
    public Response<?> deactivateOwnAccount() {

        log.info("INSIDE deactivateOwnAccount()");

        User user = getCurrentLoggedInUser();

        // Deactivate the user
        user.setActive(false);
        userRepository.save(user);

        //SEND EMAIL AFTER DEACTIVATION

        // Send email notification
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Account Deactivated")
                .body("Your account has been deactivated. If this was a mistake, please contact support.")
                .isHtml(false)
                .build();
        notificationService.sendEmail(notificationDTO);

        // Return a success response
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account deactivated successfully")
                .build();

    }

    @Override
    public Response<List<String>> getSavedEmails() {
        User user = getCurrentLoggedInUser();
        List<String> emails = user.getSavedEmails();
        if (emails == null) {
            emails = new java.util.ArrayList<>();
        }
        return Response.<List<String>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("success")
                .data(emails)
                .build();
    }

    @Override
    public Response<?> addSavedEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        String norm = email.trim().toLowerCase();
        if (!norm.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BadRequestException("Invalid email format");
        }
        User user = getCurrentLoggedInUser();
        List<String> emails = user.getSavedEmails();
        if (emails == null) {
            emails = new java.util.ArrayList<>();
            user.setSavedEmails(emails);
        }
        if (!emails.contains(norm)) {
            emails.add(norm);
            userRepository.save(user);
        }
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Saved")
                .build();
    }

    @Override
    public Response<UserDTO> updateUserRoles(Long userId, List<String> roleNames) {

        log.info("INSIDE updateUserRoles() for userId: {}", userId);

        // Normalize incoming role names: trim, uppercase, strip optional ROLE_ prefix, remove duplicates
        List<String> normalized = (roleNames == null ? List.<String>of() : roleNames).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .map(s -> s.startsWith("ROLE_") ? s.substring(5) : s)
                .distinct()
                .toList();

        if (normalized.isEmpty()) {
            throw new BadRequestException("At least one valid role must be provided");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Resolve roles, collect unknowns to return a clear 400 instead of generic 500
        java.util.List<Role> roles = new java.util.ArrayList<>();
        java.util.List<String> unknown = new java.util.ArrayList<>();
        for (String name : normalized) {
            roleRepository.findByName(name)
                    .ifPresentOrElse(roles::add, () -> unknown.add(name));
        }
        if (!unknown.isEmpty()) {
            throw new BadRequestException("Unknown roles: " + String.join(", ", unknown));
        }

        user.setRoles(roles);
        userRepository.save(user);

        UserDTO dto = modelMapper.map(user, UserDTO.class);

        return Response.<UserDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User roles updated successfully")
                .data(dto)
                .build();
    }
}















