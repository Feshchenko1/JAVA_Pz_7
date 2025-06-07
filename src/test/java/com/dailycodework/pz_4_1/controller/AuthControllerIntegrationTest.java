package com.dailycodework.pz_4_1.controller;

import com.dailycodework.pz_4_1.model.User;
import com.dailycodework.pz_4_1.payload.request.SignupRequest;
import com.dailycodework.pz_4_1.payload.response.MessageResponse;
import com.dailycodework.pz_4_1.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional // Ensures test data is rolled back
@ActiveProfiles("test") // Uses application-test.properties
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerUser_shouldCreateNewUserAndReturnSuccessMessage() throws Exception {
        // Arrange
        String username = "newTestUser";
        String email = "newtestuser@example.com";
        String password = "securePassword123";
        Set<String> roles = new HashSet<>();
        roles.add("user"); // Assign ROLE_USER by default

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(username);
        signupRequest.setEmail(email);
        signupRequest.setPassword(password);
        signupRequest.setRoles(roles);

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated()) // Expect HTTP 201 Created
                .andExpect(jsonPath("$.message").value("User registered successfully!"))
                .andReturn();

        // Assert
        Optional<User> createdUser = userRepository.findByUsername(username);
        assertThat(createdUser).isPresent();
        assertThat(createdUser.get().getUsername()).isEqualTo(username);
        assertThat(createdUser.get().getEmail()).isEqualTo(email);
        // Verify password is encrypted
        assertThat(passwordEncoder.matches(password, createdUser.get().getPassword())).isTrue();
        // Verify roles are assigned
        assertThat(createdUser.get().getRoles()).anyMatch(role -> role.getName().equals("ROLE_USER"));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenUsernameAlreadyExists() throws Exception {
        // Arrange: Use an existing username from V3 migration
        String username = "admin"; // This user already exists from V3 migration
        String email = "anotheremail@example.com";
        String password = "somePassword";

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(username);
        signupRequest.setEmail(email);
        signupRequest.setPassword(password);
        signupRequest.setRoles(new HashSet<>());

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));

        // Verify that no new user was created with this username
        long userCount = userRepository.count(); // Assuming initial count before this test
        // This check is a bit tricky with @Transactional, but it implies the user wasn't persisted
        // A more robust check would be to explicitly find the user by a unique identifier
        // that wouldn't conflict, if possible.
        assertThat(userRepository.findByEmail(email)).isNotPresent();
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {
        // Arrange: Use an existing email from V3 migration
        String username = "anotherNewUser";
        String email = "admin@example.com"; // This email already exists
        String password = "somePassword";

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(username);
        signupRequest.setEmail(email);
        signupRequest.setPassword(password);
        signupRequest.setRoles(new HashSet<>());

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"));

        // Verify no new user was created with this email
        assertThat(userRepository.findByUsername(username)).isNotPresent();
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenSignupRequestIsInvalid() throws Exception {
        // Arrange: Invalid SignupRequest (e.g., username too short)
        SignupRequest invalidSignupRequest = new SignupRequest();
        invalidSignupRequest.setUsername("ab"); // Too short (min 3)
        invalidSignupRequest.setEmail("invalid@example.com");
        invalidSignupRequest.setPassword("password");
        invalidSignupRequest.setRoles(new HashSet<>()); // Set to avoid NullPointerException if not provided by default

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSignupRequest)))
                .andExpect(status().isBadRequest())
                // Змінюємо очікування: тепер ми чекаємо конкретне поле помилки, наприклад "username"
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.username").value("Username must be between 3 and 20 characters"));
    }
}