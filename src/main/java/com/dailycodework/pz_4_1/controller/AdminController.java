package com.dailycodework.pz_4_1.controller;

import com.dailycodework.pz_4_1.dto.UpdateUserRolesRequestDTO;
import com.dailycodework.pz_4_1.dto.UserAdminViewDTO;
import com.dailycodework.pz_4_1.model.Role;
import com.dailycodework.pz_4_1.model.User;
import com.dailycodework.pz_4_1.repository.RoleRepository;
import com.dailycodework.pz_4_1.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserAdminViewDTO>> getAllUsers() {
        List<UserAdminViewDTO> users = userRepository.findAll().stream()
                .map(UserAdminViewDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAdminViewDTO> getUserById(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return ResponseEntity.ok(UserAdminViewDTO.fromEntity(user));
    }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<UserAdminViewDTO> updateUserRoles(@PathVariable Long userId,
                                                            @Valid @RequestBody UpdateUserRolesRequestDTO rolesRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Set<Role> newRoles = new HashSet<>();
        for (String roleName : rolesRequest.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));
            newRoles.add(role);
        }

        user.setRoles(newRoles);
        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(UserAdminViewDTO.fromEntity(updatedUser));
    }


}