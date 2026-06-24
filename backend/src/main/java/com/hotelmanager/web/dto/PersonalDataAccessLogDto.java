package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.DataAccessAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalDataAccessLogDto {
    private Long id;
    private Long userId;
    private Long guestId;
    private DataAccessAction action;
    private String justification;
    private Instant createdAt;
}
