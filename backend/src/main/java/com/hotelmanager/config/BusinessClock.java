package com.hotelmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class BusinessClock {

    private final ZoneId zone;

    public BusinessClock(@Value("${app.timezone:America/Mexico_City}") String timezone) {
        this.zone = ZoneId.of(timezone);
    }

    public ZoneId getZone() {
        return zone;
    }

    public LocalDate today() {
        return LocalDate.now(zone);
    }
}
