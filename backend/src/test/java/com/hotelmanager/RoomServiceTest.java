package com.hotelmanager;

import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.service.RoomService;
import com.hotelmanager.web.dto.RoomDto;
import com.hotelmanager.web.dto.RoomObservationsRequest;
import com.hotelmanager.web.dto.RoomStatusUpdateRequest;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RoomServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;

    private RoomType rt;

    @BeforeEach
    void setup() {
        rt = TestData.roomType(roomTypeRepository, "Room-Svc-Test", 2, new BigDecimal("100.00"));
    }

    private Long createRoom(RoomStatus status) {
        Room r = TestData.room(roomRepository, rt, "R-" + System.nanoTime(), 1, status);
        return r.getId();
    }

    @Test
    void availableToMaintenanceIsValid() {
        Long id = createRoom(RoomStatus.AVAILABLE);
        RoomDto dto = roomService.updateStatus(id, new RoomStatusUpdateRequest(RoomStatus.MAINTENANCE, null));
        assertEquals(RoomStatus.MAINTENANCE, dto.getStatus());
    }

    @Test
    void maintenanceToAvailableIsValid() {
        Long id = createRoom(RoomStatus.MAINTENANCE);
        RoomDto dto = roomService.updateStatus(id, new RoomStatusUpdateRequest(RoomStatus.AVAILABLE, null));
        assertEquals(RoomStatus.AVAILABLE, dto.getStatus());
    }

    @Test
    void cleaningToAvailableIsValid() {
        Long id = createRoom(RoomStatus.CLEANING);
        RoomDto dto = roomService.updateStatus(id, new RoomStatusUpdateRequest(RoomStatus.AVAILABLE, null));
        assertEquals(RoomStatus.AVAILABLE, dto.getStatus());
    }

    @Test
    void maintenanceToOccupiedIsInvalid() {
        Long id = createRoom(RoomStatus.MAINTENANCE);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> roomService.updateStatus(id, new RoomStatusUpdateRequest(RoomStatus.OCCUPIED, null)));
        assertEquals(ErrorCode.INVALID_STATE_TRANSITION, ex.getCode());
    }

    @Test
    void updateObservationsSetsValue() {
        Long id = createRoom(RoomStatus.AVAILABLE);
        RoomDto dto = roomService.updateObservations(id, new RoomObservationsRequest("Broken lamp"));
        assertEquals("Broken lamp", dto.getObservations());
    }
}
