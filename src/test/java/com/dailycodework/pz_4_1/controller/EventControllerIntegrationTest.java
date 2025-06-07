package com.dailycodework.pz_4_1.controller;

import com.dailycodework.pz_4_1.model.*;
import com.dailycodework.pz_4_1.payload.request.LoginRequest;
import com.dailycodework.pz_4_1.payload.response.JwtResponse;
import com.dailycodework.pz_4_1.repository.EventRepository;
import com.dailycodework.pz_4_1.repository.RoleRepository;
import com.dailycodework.pz_4_1.repository.UserRepository;
import com.dailycodework.pz_4_1.repository.VenueRepository;
import com.dailycodework.pz_4_1.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional // Rollback changes after each test
@ActiveProfiles("test") // Use application-test.properties
public class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    // Repositories for setting up users if needed, though V3 handles it
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired // <--- ADD THIS LINE
    private UserDetailsServiceImpl userDetailsService;

    private String adminJwtToken;
    private Long testVenueId;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure test venue exists for event creation/update
        Optional<Venue> existingVenue = venueRepository.findByName("Test Venue");
        if (existingVenue.isEmpty()) {
            Venue venue = new Venue();
            venue.setName("Test Venue");
            venue.setAddress("123 Test Street");
            venue.setCapacity(500);
            venue.setCreatedDate(java.time.LocalDateTime.now());
            venue.setLastModifiedDate(java.time.LocalDateTime.now());
            venue.setCreatedBy("TEST_SETUP");
            venue.setLastModifiedBy("TEST_SETUP");
            venueRepository.save(venue);
            testVenueId = venue.getId();
        } else {
            testVenueId = existingVenue.get().getId();
        }

        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("123123");

        // Programmatic user and role setup (this is good)
        Optional<User> adminUserOpt = userRepository.findByUsername("admin");
        User adminUser;
        if (adminUserOpt.isEmpty()) {
            adminUser = new User("admin", passwordEncoder.encode("123123"), "admin@example.com");
            userRepository.save(adminUser);
        } else {
            adminUser = adminUserOpt.get();
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN") // Use String name directly
                .orElseThrow(() -> new RuntimeException("Error: Role 'ROLE_ADMIN' not found."));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role 'ROLE_USER' not found."));

        boolean rolesAdded = false;
        if (!adminUser.getRoles().contains(adminRole)) {
            adminUser.addRole(adminRole);
            rolesAdded = true;
        }
        if (!adminUser.getRoles().contains(userRole)) {
            adminUser.addRole(userRole);
            rolesAdded = true;
        }
        if (rolesAdded) { // Only save if changes were made
            userRepository.save(adminUser);
        }
        // End programmatic user and role setup

        MvcResult loginResult = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andDo(result -> System.out.println("Login Response Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Login Response Body: " + result.getResponse().getContentAsString()))
                .andReturn();

        if (loginResult.getResponse().getStatus() != 200) {
            throw new AssertionError("Login failed with status " + loginResult.getResponse().getStatus() +
                    ". Response: " + loginResult.getResponse().getContentAsString());
        }

        String responseString = loginResult.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseString, JwtResponse.class);
        adminJwtToken = jwtResponse.getAccessToken();
        System.out.println("Admin JWT Roles: " + jwtResponse.getRoles());

        // --- NEW: Manually set SecurityContext for auditing purposes ---
        // Load UserDetails directly after successful login to ensure it has roles
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // --- END NEW ---
    }
    @Test
    @Sql(scripts = "/scripts/insert-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createEvent_shouldReturnCreatedEventAndSaveToDb_whenAuthenticatedAsAdmin() throws Exception {
        // Arrange
        String eventName = "New Test Event";
        LocalDate eventDate = LocalDate.now().plusDays(30);

        Event eventToCreate = new Event();
        eventToCreate.setName(eventName);
        eventToCreate.setEventDate(eventDate);
        // Venue will be set by the service using venueId from request param
        // No need to set createdDate/lastModifiedDate/createdBy/lastModifiedBy here as JPA Auditing handles it

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("venueId", testVenueId.toString()) // Pass venueId as request parameter
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventToCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(eventName))
                .andExpect(jsonPath("$.eventDate").value(eventDate.toString()))
                .andExpect(jsonPath("$.venue.id").value(testVenueId));

        // Verify changes in the database
        Optional<Event> createdEvent = eventRepository.findByName(eventName).stream().findFirst();
        assertThat(createdEvent).isPresent();
        assertThat(createdEvent.get().getName()).isEqualTo(eventName);
        assertThat(createdEvent.get().getEventDate()).isEqualTo(eventDate);
        assertThat(createdEvent.get().getVenue().getId()).isEqualTo(testVenueId);
        // Verify audit fields are populated
        assertThat(createdEvent.get().getCreatedDate()).isNotNull();
        assertThat(createdEvent.get().getLastModifiedDate()).isNotNull();
        assertThat(createdEvent.get().getCreatedBy()).isEqualTo("admin"); // Assumes AuditAwareImpl returns "admin" for tests
    }

    @Test
    @Sql(scripts = "/scripts/insert-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateEvent_shouldReturnUpdatedEventAndSaveToDb_whenAuthenticatedAsAdmin() throws Exception {
        // Arrange: Create an event first to update it
        Event existingEvent = new Event();
        existingEvent.setName("Event to Update");
        existingEvent.setEventDate(LocalDate.now().plusDays(10));
        existingEvent.setVenue(venueRepository.findById(testVenueId).orElseThrow()); // Use the setup venue
        existingEvent.setCreatedDate(java.time.LocalDateTime.now());
        existingEvent.setLastModifiedDate(java.time.LocalDateTime.now());
        existingEvent.setCreatedBy("TEST");
        existingEvent.setLastModifiedBy("TEST");
        eventRepository.save(existingEvent);

        String updatedName = "Updated Event Name";
        LocalDate updatedDate = LocalDate.now().plusDays(40);

        Event eventDetails = new Event();
        eventDetails.setName(updatedName);
        eventDetails.setEventDate(updatedDate);
        // No need to set venue, it's passed as param

        // Act & Assert
        mockMvc.perform(put("/api/events/{id}", existingEvent.getId())
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("venueId", testVenueId.toString()) // Pass venueId as request parameter
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingEvent.getId()))
                .andExpect(jsonPath("$.name").value(updatedName))
                .andExpect(jsonPath("$.eventDate").value(updatedDate.toString()));

        // Verify changes in the database
        Optional<Event> updatedEvent = eventRepository.findById(existingEvent.getId());
        assertThat(updatedEvent).isPresent();
        assertThat(updatedEvent.get().getName()).isEqualTo(updatedName);
        assertThat(updatedEvent.get().getEventDate()).isEqualTo(updatedDate);
        assertThat(updatedEvent.get().getLastModifiedBy()).isEqualTo("admin"); // Check last modified by
    }

    @Test
    @Sql(scripts = "/scripts/insert-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createEvent_shouldReturnForbidden_whenAuthenticatedAsUser() throws Exception {
        // Arrange: Authenticate as a regular user
        LoginRequest userLogin = new LoginRequest();
        userLogin.setUsername("user");
        userLogin.setPassword("123123"); // Password from V3 migration script

        MvcResult loginResult = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = loginResult.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseString, JwtResponse.class);
        String userJwtToken = jwtResponse.getAccessToken();

        String eventName = "Event by User";
        LocalDate eventDate = LocalDate.now().plusDays(30);

        Event eventToCreate = new Event();
        eventToCreate.setName(eventName);
        eventToCreate.setEventDate(eventDate);

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + userJwtToken)
                        .param("venueId", testVenueId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventToCreate)))
                .andExpect(status().isForbidden()); // Expect 403 Forbidden

        // Verify that event was NOT created in the database
        Optional<Event> createdEvent = eventRepository.findByName(eventName).stream().findFirst();
        assertThat(createdEvent).isNotPresent();
    }

    @Test
    void createEvent_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {
        // Arrange
        String eventName = "Unauthorized Event";
        LocalDate eventDate = LocalDate.now().plusDays(30);

        Event eventToCreate = new Event();
        eventToCreate.setName(eventName);
        eventToCreate.setEventDate(eventDate);

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .param("venueId", testVenueId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventToCreate)))
                .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized

        // Verify that event was NOT created in the database
        Optional<Event> createdEvent = eventRepository.findByName(eventName).stream().findFirst();
        assertThat(createdEvent).isNotPresent();
    }
}