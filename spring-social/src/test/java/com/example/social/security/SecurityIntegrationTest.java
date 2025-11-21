package com.example.social.security;

import com.example.social.security.model.AuthUser;
import com.example.social.security.repository.AuthUserRepository;
import com.example.social.model.User;
import com.example.social.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AuthUserRepository authUserRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;

    Long socialUserId;

    @BeforeEach
    void setup() {
        authUserRepository.deleteAll();
        userRepository.deleteAll();

        User socialUser = new User();
        socialUser.setUsername("u1");
        socialUser.setName("U1");
        socialUserId = userRepository.save(socialUser).getId();

        AuthUser user = new AuthUser();
        user.setLogin("user1");
        user.setPasswordHash(passwordEncoder.encode("Strong!Pass1"));
        user.setRoles(Set.of("ROLE_USER"));
        authUserRepository.save(user);

        AuthUser admin = new AuthUser();
        admin.setLogin("admin1");
        admin.setPasswordHash(passwordEncoder.encode("Strong!Pass2"));
        Set<String> roles = new LinkedHashSet<>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_USER");
        admin.setRoles(roles);
        authUserRepository.save(admin);
    }

    @Test
    void register_isPublic_andRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"new1\",\"password\":\"weak\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"new1\",\"password\":\"Strong!Pass3\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_requireAuth() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/posts").with(httpBasic("user1", "Strong!Pass1")))
                .andExpect(status().isOk());
    }

    @Test
    void usersApi_isAdminOnly() throws Exception {
        mockMvc.perform(get("/api/users").with(httpBasic("user1", "Strong!Pass1")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").with(httpBasic("admin1", "Strong!Pass2")))
                .andExpect(status().isOk());
    }

    @Test
    void postCreate_requiresCsrf() throws Exception {
        String body = "{\"userId\":" + socialUserId + ",\"text\":\"hi\"}";

        mockMvc.perform(post("/api/posts")
                .with(httpBasic("user1", "Strong!Pass1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/posts")
                .with(httpBasic("user1", "Strong!Pass1"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }
}
