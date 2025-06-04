package com.dailycodework.pz_4_1.controller;

import com.dailycodework.pz_4_1.model.Role;
import com.dailycodework.pz_4_1.model.User;
import com.dailycodework.pz_4_1.payload.request.SignupRequest;
import com.dailycodework.pz_4_1.repository.RoleRepository;
import com.dailycodework.pz_4_1.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
//@Sql(value = "/scripts/insert-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;


    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(new Role("ROLE_USER"));
        roleRepository.save(new Role("ROLE_ADMIN"));

        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword("123123");
        adminUser.setEmail("admin@test.com");
        adminUser.setEnabled(true);
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        adminUser.setRoles(roles);
        userRepository.save(adminUser);
    }

    @Test
    @DisplayName("registerUser повернути статус створеного та зберегти користувача, коли запит на реєстрацію дійсний")
    void registerUser_shouldReturnCreatedStatus() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser_test");
        signupRequest.setEmail("newuser_test@example.com");
        signupRequest.setPassword("securepassword");
        signupRequest.setRoles(Collections.singleton("user"));
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        Optional<User> registeredUser = userRepository.findByUsername("newuser_test");
        assertThat(registeredUser).isPresent();
        assertThat(registeredUser.get().getEmail()).isEqualTo("newuser_test@example.com");
        assertThat(registeredUser.get().getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_USER"))).isTrue();
    }

    @Test
    @DisplayName("registerUser повинен повертати неправильний запит, якщо ім'я користувача вже існує")
    void registerUser_shouldReturnBadRequest() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("admin");
        signupRequest.setEmail("another@example.com");
        signupRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));

        assertThat(userRepository.findByEmail("another@example.com")).isNotPresent();
    }

    @Test
    @DisplayName("registerUser повинен повертати неправильний запит, якщо електронна пошта вже існує")
    void registerUser_shouldReturnBadRequest_Email() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("new_unique_user");
        signupRequest.setEmail("admin@test.com");
        signupRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"));

        assertThat(userRepository.findByUsername("new_unique_user")).isNotPresent();
    }
}
