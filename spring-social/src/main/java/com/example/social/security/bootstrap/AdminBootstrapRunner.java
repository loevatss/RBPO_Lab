package com.example.social.security.bootstrap;

import com.example.social.security.model.AuthUser;
import com.example.social.security.repository.AuthUserRepository;
import com.example.social.security.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.bootstrap-admin.enabled:false}")
    private boolean enabled;

    @Value("${app.security.bootstrap-admin.login:}")
    private String login;

    @Value("${app.security.bootstrap-admin.password:}")
    private String password;

    public AdminBootstrapRunner(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (login == null || login.isBlank()) {
            return;
        }
        if (password == null || password.isBlank()) {
            return;
        }
        if (authUserRepository.existsByLogin(login)) {
            return;
        }

        AuthUser u = new AuthUser();
        u.setLogin(login);
        u.setPasswordHash(passwordEncoder.encode(password));

        Set<String> roles = new LinkedHashSet<>();
        roles.add(AuthService.ROLE_ADMIN);
        roles.add(AuthService.ROLE_USER);
        u.setRoles(roles);

        authUserRepository.save(u);
    }
}
