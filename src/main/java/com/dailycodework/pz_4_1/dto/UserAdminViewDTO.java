package com.dailycodework.pz_4_1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import com.dailycodework.pz_4_1.model.User;


@Getter
@Setter
@NoArgsConstructor

public class UserAdminViewDTO {
    private Long id;
    private String username;
    private String email;
    private boolean enabled;
    private Set<String> roles;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private String createdBy;
    private String lastModifiedBy;

    public UserAdminViewDTO(Long id, String username, String email, boolean enabled,
                            Set<String> roles, LocalDateTime createdDate,
                            LocalDateTime lastModifiedDate, String createdBy, String lastModifiedBy) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.enabled = enabled;
        this.roles = roles;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
    }
    public static UserAdminViewDTO fromEntity(User user) {
        return new UserAdminViewDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()),
                user.getCreatedDate(),
                user.getLastModifiedDate(),
                user.getCreatedBy(),
                user.getLastModifiedBy()
        );

    }

}