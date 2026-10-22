package com.toghrakx.backend.config;

import com.toghrakx.backend.exception.ResourceNotFoundException;
import com.toghrakx.backend.model.Role;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.model.Page;
import com.toghrakx.backend.model.SiteSettings;
import com.toghrakx.backend.model.SocialLink;
import com.toghrakx.backend.repository.RoleRepository;
import com.toghrakx.backend.repository.UserRepository;
import com.toghrakx.backend.repository.PageRepository;
import com.toghrakx.backend.repository.SiteSettingsRepository;
import com.toghrakx.backend.repository.SocialLinkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.regex.Pattern;

@Configuration
@Slf4j
public class DataInitializer {

    // Basic email format check — must contain a local part, @, and a domain with a dot.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

        @Value("${default.admin.username:}")
        private String adminUsername;

        @Value("${default.admin.email:}")
        private String adminEmail;

        @Value("${default.admin.password:}")
        private String adminPassword;

        @Bean
        public CommandLineRunner initDatabase(RoleRepository roleRepo,
                        UserRepository userRepo,
                        PasswordEncoder passwordEncoder,
                        PageRepository pageRepository,
                        SocialLinkRepository socialLinkRepository,
                        SiteSettingsRepository siteSettingsRepository) {
                return args -> {
                        // Seed roles if they do not exist
                        if (roleRepo.findByName("ADMIN").isEmpty()) {
                                roleRepo.save(Role.builder().name("ADMIN").build());
                        }
                        if (roleRepo.findByName("EDITOR").isEmpty()) {
                                roleRepo.save(Role.builder().name("EDITOR").build());
                        }
                        log.info("DataInitializer: Roles seeded successfully.");

                        // Seed a default admin user if none exists
                        if (userRepo.findByRole_Name("ADMIN").isEmpty()) {
                                // Ensure the injected admin credentials are present before attempting to create
                                if (adminUsername == null || adminUsername.isBlank() ||
                                                adminEmail == null || adminEmail.isBlank() ||
                                                adminPassword == null || adminPassword.isBlank()) {
                                        log.warn("Default admin credentials not fully configured (username/email/password); skipping admin seed.");
                                } else if (!EMAIL_PATTERN.matcher(adminEmail).matches()) {
                                        log.warn("Default admin email '{}' is not a valid email address; skipping admin seed.", adminEmail);
                                } else {
                                        Role adminRole = roleRepo.findByName("ADMIN")
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                        "Admin role not found"));

                                        User admin = User.builder()
                                                        .username(adminUsername)
                                                        .email(adminEmail.trim().toLowerCase(Locale.ROOT))
                                                        .password(passwordEncoder.encode(adminPassword))
                                                        .role(adminRole)
                                                        .build();

                                        userRepo.save(admin);
                                        log.info("DataInitializer: Default admin user seeded successfully.");
                                }
                        }

                        // Seed default pages if missing
                        if (pageRepository.count() == 0) {
                                List<Page> defaults = List.of(
                                                Page.builder().slug("about").title("About")
                                                                .content("Tell readers about Toghrak X.")
                                                                .updatedAt(Instant.now()).build(),
                                                Page.builder().slug("contact").title("Contact")
                                                                .content("Email: hello@toghrakpublishingplatform.example")
                                                                .updatedAt(Instant.now()).build(),
                                                Page.builder().slug("how-it-works").title("How It Works")
                                                                .content("Explain how Toghrak X works for creators and readers.")
                                                                .updatedAt(Instant.now()).build(),
                                                Page.builder().slug("faq").title("FAQ")
                                                                .content("Add common questions and answers.")
                                                                .updatedAt(Instant.now()).build(),
                                                Page.builder().slug("privacy").title("Privacy Policy")
                                                                .content("Describe how user data is handled.")
                                                                .updatedAt(Instant.now()).build(),
                                                Page.builder().slug("terms").title("Terms of Use")
                                                                .content("Outline acceptable use and responsibilities.")
                                                                .updatedAt(Instant.now())
                                                                .build());
                                defaults.forEach(pageRepository::save);
                        }

                        if (socialLinkRepository.count() == 0) {
                                socialLinkRepository.saveAll(List.of(
                                                SocialLink.builder().label("Twitter")
                                                                .url("https://twitter.com/toghrakplatform").icon("x")
                                                                .build(),
                                                SocialLink.builder().label("Facebook")
                                                                .url("https://facebook.com/toghrakplatform").icon("f")
                                                                .build(),
                                                SocialLink.builder().label("LinkedIn")
                                                                .url("https://linkedin.com/company/toghrakplatform")
                                                                .icon("in").build()));
                        }

                        // The site has exactly one settings row; create it here so GET /api/site-settings only reads.
                        if (siteSettingsRepository.count() == 0) {
                                siteSettingsRepository.save(new SiteSettings());
                        }
                };
        }
}
