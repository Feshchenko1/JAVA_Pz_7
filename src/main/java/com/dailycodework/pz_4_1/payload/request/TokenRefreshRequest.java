package com.dailycodework.pz_4_1.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRefreshRequest {
    @NotBlank(message = "Refresh token cannot be empty")
    private String refreshToken;
}
