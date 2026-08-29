package com.exelynt.booking.config;

import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.Role;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@exelynt.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Seeded ADMIN user -> username: admin | password: Admin@123");
        }

        if (!userRepository.existsByUsername("user")) {
            User user = User.builder()
                    .username("user")
                    .email("user@exelynt.com")
                    .password(passwordEncoder.encode("User@123"))
                    .role(Role.USER)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Seeded USER user -> username: user | password: User@123");
        }
    }

    private void seedResources() {
        if (resourceRepository.count() > 0) {
            return;
        }

        resourceRepository.save(Resource.builder()
                .name("Conference Room A")
                .description("Large conference room with projector and video conferencing")
                .type("ROOM")
                .location("Building 1, Floor 2")
                .pricePerHour(new BigDecimal("25.00"))
                .available(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Toyota Innova")
                .description("7-seater vehicle for company travel")
                .type("VEHICLE")
                .location("Basement Parking")
                .pricePerHour(new BigDecimal("15.00"))
                .available(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Projector - Epson EB-X05")
                .description("Portable projector for presentations")
                .type("EQUIPMENT")
                .location("Store Room")
                .pricePerHour(new BigDecimal("5.00"))
                .available(true)
                .build());

        log.info("Seeded 3 sample resources");
    }
}
