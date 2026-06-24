package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.PersonalDataAccessLog;
import com.hotelmanager.web.dto.PersonalDataAccessLogDto;

public final class PersonalDataAccessLogMapper {

    private PersonalDataAccessLogMapper() {
    }

    public static PersonalDataAccessLogDto toDto(PersonalDataAccessLog log) {
        if (log == null) {
            return null;
        }
        return new PersonalDataAccessLogDto(
                log.getId(),
                log.getUserId(),
                log.getGuestId(),
                log.getAction(),
                log.getJustification(),
                log.getCreatedAt()
        );
    }
}
