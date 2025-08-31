package top.ajasta.AjastaApp.config;

import top.ajasta.AjastaApp.role.entity.Role;
import top.ajasta.AjastaApp.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnsureBookingRolesInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Ensure roles needed for booking app exist regardless of previous seeding
        List<String> requiredRoles = Arrays.asList("ADMIN", "CUSTOMER", "DELIVERY");
        Set<String> existing = new HashSet<>();
        roleRepository.findAll().forEach(r -> existing.add(r.getName()));
        boolean created = false;
        for (String name : requiredRoles) {
            if (!existing.contains(name)) {
                roleRepository.save(Role.builder().name(name).build());
                created = true;
                log.info("Created missing role: {}", name);
            }
        }
        if (!created) {
            log.info("All required roles already exist");
        }
    }
}
