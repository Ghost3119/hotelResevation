package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.User;
import com.hotelmanager.web.dto.UserDto;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getActive());
    }
}
