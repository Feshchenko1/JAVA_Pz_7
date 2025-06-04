package com.dailycodework.pz_4_1.controller;

import com.dailycodework.pz_4_1.model.Event;
import com.dailycodework.pz_4_1.model.Role;
import com.dailycodework.pz_4_1.model.User;
import com.dailycodework.pz_4_1.model.Venue;
import com.dailycodework.pz_4_1.payload.request.LoginRequest;
import com.dailycodework.pz_4_1.payload.response.JwtResponse;
import com.dailycodework.pz_4_1.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
@Sql(value = "/scripts/insert-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;
    private Long testVenueId;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper.registerModule(new JavaTimeModule());

        eventRepository.deleteAll();
        venueRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();


        Role roleAdminEntity = roleRepository.save(new Role("ROLE_ADMIN"));
        Role roleUserEntity = roleRepository.save(new Role("ROLE_USER"));

        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("123123"));
        adminUser.setEmail("admin@test.com");
        adminUser.setEnabled(true);
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(roleAdminEntity);
        adminUser.setRoles(adminRoles);
        userRepository.save(adminUser);

        Venue venue = new Venue();
        venue.setName("Test Venue From Setup");
        venue.setAddress("123 Test Address");
        venue.setCapacity(100);
        Venue savedVenue = venueRepository.saveAndFlush(venue); // Важливо!
        this.testVenueId = savedVenue.getId();


    }

    @Test
    @DisplayName("createEvent має повертати статус створеного та зберігати подію після автентифікації за допомогою дійсного Jwt")
    void createEvent_shouldReturnCreatedStatus() throws Exception {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("123123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = loginResult.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseString, JwtResponse.class);
        String token = jwtResponse.getAccessToken();

        Optional<Venue> existingVenue = venueRepository.findById(1L);
        assertThat(existingVenue).isPresent();

        Event eventToCreate = new Event();
        eventToCreate.setName("New Test Event");
        eventToCreate.setEventDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("venueId", this.testVenueId.toString()) // Використовуй збережений ID
                        .content(objectMapper.writeValueAsString(eventToCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Test Event"))
                .andExpect(jsonPath("$.eventDate").value(eventToCreate.getEventDate().toString()))
                .andExpect(jsonPath("$.venue.id").value(this.testVenueId));

        Optional<Event> createdEvent = eventRepository.findByName("New Test Event").stream().findFirst();
        assertThat(createdEvent).isPresent();
        assertThat(createdEvent.get().getName()).isEqualTo("New Test Event");
        assertThat(createdEvent.get().getEventDate()).isEqualTo(LocalDate.now().plusDays(10));
        assertThat(createdEvent.get().getVenue().getId()).isEqualTo(this.testVenueId);
    }
    @Test
    @DisplayName("create Event має повертати неавторизований статус, якщо не автентифіковано")
    void createEvent_shouldReturnUnauthorizedStatus() throws Exception {
        Event eventToCreate = new Event();
        eventToCreate.setName("Unauthorized Event");
        eventToCreate.setEventDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("venueId", "1")
                        .content(objectMapper.writeValueAsString(eventToCreate)))
                .andExpect(status().isUnauthorized());

        assertThat(eventRepository.findByName("Unauthorized Event")).isEmpty();
    }
}
