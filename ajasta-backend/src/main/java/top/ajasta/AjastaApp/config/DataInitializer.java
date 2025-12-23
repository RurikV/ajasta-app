package top.ajasta.AjastaApp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.auth_users.repository.UserRepository;
import top.ajasta.AjastaApp.role.entity.Role;
import top.ajasta.AjastaApp.role.repository.RoleRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            log.info("Initializing database with sample data...");
            initializeRoles();
            initializeUsers();
            log.info("Database initialization completed successfully!");
        } else {
            log.info("Database already contains data. Skipping initialization.");
        }
    }

    private void initializeRoles() {
        List<Role> roles = Arrays.asList(
                Role.builder().name("ADMIN").build(),
                Role.builder().name("CUSTOMER").build(),
                Role.builder().name("RESOURCE_MANAGER").build()
        );
        roleRepository.saveAll(roles);
        log.info("Initialized {} roles", roles.size());
    }

    private void initializeUsers() {
        List<Role> roles = roleRepository.findAll();
        Role userRole = roles.stream().filter(r -> r.getName().equals("CUSTOMER")).findFirst().orElse(null);
        Role adminRole = roles.stream().filter(r -> r.getName().equals("ADMIN")).findFirst().orElse(null);
        Role managerRole = roles.stream().filter(r -> r.getName().equals("RESOURCE_MANAGER")).findFirst().orElse(null);

        List<User> users = Arrays.asList(
                User.builder()
                        .name("John Doe")
                        .email("john.doe@example.com")
                        .password(passwordEncoder.encode("password123"))
                        .phoneNumber("+1234567890")
                        .address("123 Main St, New York, NY 10001")
                        .isActive(true)
                        .roles(Arrays.asList(userRole))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                User.builder()
                        .name("Jane Smith")
                        .email("jane.smith@example.com")
                        .password(passwordEncoder.encode("password123"))
                        .phoneNumber("+1234567891")
                        .address("456 Oak Ave, Los Angeles, CA 90210")
                        .isActive(true)
                        .roles(Arrays.asList(userRole))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                User.builder()
                        .name("Admin User")
                        .email("admin@ajastaapp.com")
                        .password(passwordEncoder.encode("admin123"))
                        .phoneNumber("+1234567892")
                        .address("789 Admin Blvd, Chicago, IL 60601")
                        .isActive(true)
                        .roles(Arrays.asList(adminRole, userRole))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                User.builder()
                        .name("Restaurant Manager")
                        .email("manager@ajastaapp.com")
                        .password(passwordEncoder.encode("manager123"))
                        .phoneNumber("+1234567893")
                        .address("321 Restaurant St, Miami, FL 33101")
                        .isActive(true)
                        .roles(Arrays.asList(managerRole, userRole))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                User.builder()
                        .name("Alice Johnson")
                        .email("alice.johnson@example.com")
                        .password(passwordEncoder.encode("password123"))
                        .phoneNumber("+1234567894")
                        .address("654 Pine St, Seattle, WA 98101")
                        .isActive(true)
                        .roles(Arrays.asList(userRole))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                User.builder()
                        .name("Bob Wilson")
                        .email("bob.wilson@example.com")
                        .password(passwordEncoder.encode("password123"))
                        .phoneNumber("+1234567895")
                        .address("987 Elm St, Boston, MA 02101")
                        .isActive(true)
                        .roles(Arrays.asList(userRole))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        userRepository.saveAll(users);
        log.info("Initialized {} users", users.size());
    }
}