package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Email
    @Size(max = 254)
    private String email;

    @Size(min = 14, max = 72, message = "Password must be between 14 and 72 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])[\\x21-\\x7E]+$",
            message = "Password must include upper and lower case, a number, and a symbol"
    )
    private String password;

    @NotBlank
    @Size(max = 200)
    private String fullName;

    private UserRole role;

    private Boolean active;
}
