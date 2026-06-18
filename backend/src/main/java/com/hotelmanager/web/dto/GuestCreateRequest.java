package com.hotelmanager.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuestCreateRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String email;

    private String phone;

    @NotBlank
    private String documentNumber;

    private String nationality;
}
