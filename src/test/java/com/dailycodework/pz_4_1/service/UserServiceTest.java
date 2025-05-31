package com.dailycodework.pz_4_1.service;

import com.dailycodework.pz_4_1.model.Role;
import com.dailycodework.pz_4_1.model.User;
import com.dailycodework.pz_4_1.payload.request.SignupRequest;
import com.dailycodework.pz_4_1.repository.RoleRepository;
import com.dailycodework.pz_4_1.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private SignupRequest signupRequest;
    private Role userRoleEntity;
    private Role adminRoleEntity;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");

        userRoleEntity = new Role("ROLE_USER");
        userRoleEntity.setId(1L);

        adminRoleEntity = new Role("ROLE_ADMIN");
        adminRoleEntity.setId(2L);
    }

    @Test
    @DisplayName("Успішна реєстрація користувача з роллю USER за замовчуванням")
    void registerUser_whenTypicalRequest() {
        signupRequest.setRoles(null);
        String hashedPassword = "hashedPassword123";

        User expectedUser = new User(signupRequest.getUsername(), hashedPassword, signupRequest.getEmail());
        expectedUser.setId(1L);
        Set<Role> roles = new HashSet<>();
        roles.add(userRoleEntity);
        expectedUser.setRoles(roles);

        when(userRepository.findByUsername(signupRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRoleEntity));
        when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        User actualUser = userService.registerUser(signupRequest);

        assertNotNull(actualUser);
        assertEquals(expectedUser.getId(), actualUser.getId());
        assertEquals(signupRequest.getUsername(), actualUser.getUsername());
        assertEquals(signupRequest.getEmail(), actualUser.getEmail());
        assertEquals(hashedPassword, actualUser.getPassword());
        assertTrue(actualUser.getRoles().contains(userRoleEntity));
        assertEquals(1, actualUser.getRoles().size());

        verify(userRepository, times(1)).findByUsername(signupRequest.getUsername());
        verify(userRepository, times(1)).findByEmail(signupRequest.getEmail());
        verify(roleRepository, times(1)).findByName("ROLE_USER");
        verify(passwordEncoder, times(1)).encode(signupRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Успішна реєстрація користувача з явно вказаною роллю ADMIN")
    void registerUser_whenRequestWithAdminRole() {
        Set<String> requestedRoles = new HashSet<>();
        requestedRoles.add("admin");
        signupRequest.setRoles(requestedRoles);
        String hashedPassword = "hashedPasswordAdmin";

        User expectedUser = new User(signupRequest.getUsername(), hashedPassword, signupRequest.getEmail());
        expectedUser.setId(2L);
        Set<Role> roles = new HashSet<>();
        roles.add(adminRoleEntity);
        expectedUser.setRoles(roles);

        when(userRepository.findByUsername(signupRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRoleEntity));
        when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        User actualUser = userService.registerUser(signupRequest);

        assertNotNull(actualUser);
        assertThat(actualUser.getRoles()).contains(adminRoleEntity);
        assertEquals(1, actualUser.getRoles().size());

        verify(roleRepository, times(1)).findByName("ROLE_ADMIN");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository, times(1)).findByUsername(signupRequest.getUsername());
        verify(userRepository, times(1)).findByEmail(signupRequest.getEmail());
        verify(passwordEncoder, times(1)).encode(signupRequest.getPassword());
    }

    @Test
    @DisplayName("Реєстрація користувача, коли ім'я користувача вже зайняте, має кинути RuntimeException")
    void registerUser_whenUsernameAlreadyTaken() {
        when(userRepository.findByUsername(signupRequest.getUsername())).thenReturn(Optional.of(new User()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(signupRequest);
        });

        assertEquals("Error: Username is already taken!", exception.getMessage());

        verify(userRepository, times(1)).findByUsername(signupRequest.getUsername());
        verify(userRepository, never()).findByEmail(anyString());
        verify(roleRepository, never()).findByName(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    @DisplayName("Реєстрація користувача, коли вказана роль не існує, має кинути EntityNotFoundException")
    void registerUser_whenRoleNotFound() {
        Set<String> requestedRoles = new HashSet<>();
        String nonExistentRole = "ROLE_GUEST";
        requestedRoles.add(nonExistentRole);
        signupRequest.setRoles(requestedRoles);

        when(userRepository.findByUsername(signupRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            userService.registerUser(signupRequest);
        });

        assertTrue(exception.getMessage().contains("Error: Role '" + nonExistentRole + "' not found."));

        verify(userRepository, times(1)).findByUsername(signupRequest.getUsername());
        verify(userRepository, times(1)).findByEmail(signupRequest.getEmail());

        verify(passwordEncoder, times(1)).encode(signupRequest.getPassword());

        verify(roleRepository, never()).findByName("ROLE_USER");
        verify(roleRepository, never()).findByName("ROLE_ADMIN");
        verify(roleRepository, never()).findByName(nonExistentRole);

        verify(userRepository, never()).save(any(User.class));
    }
}