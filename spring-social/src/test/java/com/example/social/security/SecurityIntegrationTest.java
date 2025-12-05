package com.example.social.security;

import com.example.social.security.model.AuthUser;
import com.example.social.security.repository.AuthUserRepository;
import com.example.social.model.User;
import com.example.social.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        ObjectMapper objectMapper;

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
                                .andExpect(status().isBadRequest());

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"new1\",\"password\":\"Strong!Pass3\"}"))
                                .andExpect(status().isOk());
        }

        @Test
        void protectedEndpoints_requireAuth() throws Exception {
                mockMvc.perform(get("/api/posts"))
                                .andExpect(status().isForbidden());

                String accessToken = loginAndGetAccessToken("user1", "Strong!Pass1");

                mockMvc.perform(get("/api/posts")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk());
        }

        @Test
        void usersApi_isAdminOnly() throws Exception {
                String userAccess = loginAndGetAccessToken("user1", "Strong!Pass1");
                String adminAccess = loginAndGetAccessToken("admin1", "Strong!Pass2");

                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + userAccess))
                                .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + adminAccess))
                                .andExpect(status().isOk());
        }

        @Test
        void postCreate_requiresJwtButNotCsrf() throws Exception {
                String body = "{\"userId\":" + socialUserId + ",\"text\":\"hi\"}";

                mockMvc.perform(post("/api/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isForbidden());

                String access = loginAndGetAccessToken("user1", "Strong!Pass1");

                mockMvc.perform(post("/api/posts")
                                .header("Authorization", "Bearer " + access)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isOk());
        }

        @Test
        void refreshToken_cannotBeUsedTwice() throws Exception {
                TokenPair pair1 = loginAndGetTokenPair("user1", "Strong!Pass1");

                TokenPair pair2 = refreshAndGetTokenPair(pair1.refreshToken);

                // Second refresh with old refresh token must fail (reuse detection)
                mockMvc.perform(post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + pair1.refreshToken + "\"}"))
                                .andExpect(status().isForbidden());

                // New access token from second pair should work
                mockMvc.perform(get("/api/posts")
                                .header("Authorization", "Bearer " + pair2.accessToken))
                                .andExpect(status().isOk());
        }

        private String loginAndGetAccessToken(String login, String password) throws Exception {
                return loginAndGetTokenPair(login, password).accessToken;
        }

        private TokenPair loginAndGetTokenPair(String login, String password) throws Exception {
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                return new TokenPair(json.get("accessToken").asText(), json.get("refreshToken").asText());
        }

        private TokenPair refreshAndGetTokenPair(String refreshToken) throws Exception {
                MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                return new TokenPair(json.get("accessToken").asText(), json.get("refreshToken").asText());
        }

        private static class TokenPair {
                final String accessToken;
                final String refreshToken;

                private TokenPair(String accessToken, String refreshToken) {
                        this.accessToken = accessToken;
                        this.refreshToken = refreshToken;
                }
        }
}
