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
    public void run(String... args) {
        log.info("Starting role initialization for booking system...");
        
        // Ensure roles needed for booking app exist regardless of previous seeding
        List<String> requiredRoles = Arrays.asList("ADMIN", "CUSTOMER");
        Set<String> existing = new HashSet<>();
        
        try {
            roleRepository.findAll().forEach(r -> existing.add(r.getName()));
            log.info("Found existing roles: {}", existing);
            
            boolean created = false;
            for (String name : requiredRoles) {
                if (!existing.contains(name)) {
                    Role newRole = Role.builder().name(name).build();
                    roleRepository.save(newRole);
                    created = true;
                    log.info("Created missing role: {}", name);
                } else {
                    log.debug("Role already exists: {}", name);
                }
            }
            
            if (!created) {
                log.info("All required roles already exist: {}", requiredRoles);
            }
            
            // Verify roles were created successfully
            Set<String> finalRoles = new HashSet<>();
            roleRepository.findAll().forEach(r -> finalRoles.add(r.getName()));
            
            for (String requiredRole : requiredRoles) {
                if (!finalRoles.contains(requiredRole)) {
                    log.error("CRITICAL: Required role '{}' was not found after initialization!", requiredRole);
                }
            }
            
            log.info("Role initialization completed. Available roles: {}", finalRoles);
            
        } catch (Exception e) {
            log.error("Error during role initialization: {}", e.getMessage(), e);
            throw e;
        }
    }
}
