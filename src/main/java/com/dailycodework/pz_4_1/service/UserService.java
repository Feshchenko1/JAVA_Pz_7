package com.dailycodework.pz_4_1.service;

import com.dailycodework.pz_4_1.exception.TokenRefreshException;
import com.dailycodework.pz_4_1.model.Role;
import com.dailycodework.pz_4_1.model.User;
import com.dailycodework.pz_4_1.payload.request.SignupRequest;
import com.dailycodework.pz_4_1.repository.RoleRepository;
import com.dailycodework.pz_4_1.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(SignupRequest signupRequest) {
        if (userRepository.findByUsername(signupRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User(signupRequest.getUsername(),
                passwordEncoder.encode(signupRequest.getPassword()),
                signupRequest.getEmail());

        Set<String> strRoles = signupRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new EntityNotFoundException("Error: Role 'ROLE_USER' not found."));
            roles.add(userRole);
        } else {
            for (String role : strRoles) {
                switch (role.toLowerCase()) {
                    case "admin":
                        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                                .orElseThrow(() -> new EntityNotFoundException("Error: Role 'ROLE_ADMIN' not found."));
                        roles.add(adminRole);
                        break;
                    case "user":
                        Role userRole = roleRepository.findByName("ROLE_USER")
                                .orElseThrow(() -> new EntityNotFoundException("Error: Role 'ROLE_USER' not found."));
                        roles.add(userRole);
                        break;
                    default:
                        throw new EntityNotFoundException("Error: Role '" + role + "' not found.");
                }
            }
        }

        user.setRoles(roles);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }


    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
