package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    @NotBlank
    private String email;

    private String password;

    @NotBlank
    private String fullName;

    private UserRole role;

    private Boolean active;
}
