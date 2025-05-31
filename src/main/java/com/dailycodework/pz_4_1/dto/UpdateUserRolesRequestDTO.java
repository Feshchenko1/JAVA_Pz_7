package com.dailycodework.pz_4_1.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;


@Getter
@Setter
public class UpdateUserRolesRequestDTO {
    public UpdateUserRolesRequestDTO(Set<String> roles) {
        this.roles = roles;
    }

    @NotEmpty(message = "List roles isn't be empty")
    private Set<String> roles;

}