package com.hotelmanager;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.service.AvailabilityService;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.web.dto.AvailabilityRoomDto;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AvailabilityServiceTest {

    @Autowired
    private AvailabilityService availabilityService;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private GuestRepository guestRepository;

    private RoomType doble;
    private Room roomA;
    private Room roomB;
    private Room roomC;
    private Room roomD;
    private Guest guest;

    @BeforeEach
    void setup() {
        doble = TestData.roomType(roomTypeRepository, "Doble-Avl", 2, new BigDecimal("120.00"));
        roomA = TestData.room(roomRepository, doble, "A-101", 1, RoomStatus.AVAILABLE);
        roomB = TestData.room(roomRepository, doble, "A-102", 1, RoomStatus.MAINTENANCE);
        roomC = TestData.room(roomRepository, doble, "A-103", 1, RoomStatus.OUT_OF_SERVICE);
        roomD = TestData.room(roomRepository, doble, "A-104", 1, RoomStatus.AVAILABLE);
        guest = TestData.guest(guestRepository, "DOC-AVL-1");
    }

    private ReservationCreateRequest createReq(LocalDate in, LocalDate out, Long roomId) {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setGuestId(guest.getId());
        req.setCheckIn(in);
        req.setCheckOut(out);
        req.setAdults(2);
        req.setChildren(0);
        req.setRoomTypeId(doble.getId());
        req.setRoomId(roomId);
        return req;
    }

    @Test
    void searchReturnsOnlyFreeRoomsAndExcludesOverlapMaintenanceOutOfService() {
        LocalDate day1 = LocalDate.now().plusDays(1);
        LocalDate day3 = LocalDate.now().plusDays(3);
        reservationService.create(createReq(day1, day3, roomA.getId()));

        List<AvailabilityRoomDto> available = availabilityService.search(day1, day3, 2, doble.getId());
        List<Long> ids = available.stream().map(AvailabilityRoomDto::getRoomId).toList();
        assertFalse(ids.contains(roomA.getId()), "overlapping room should be excluded");
        assertFalse(ids.contains(roomB.getId()), "maintenance room should be excluded");
        assertFalse(ids.contains(roomC.getId()), "out_of_service room should be excluded");
        assertTrue(ids.contains(roomD.getId()), "free available room should be returned");
        assertEquals(1, available.size());
        assertEquals(2, available.get(0).getMaxCapacity());
    }

    @Test
    void searchExcludesRoomsWithInsufficientCapacity() {
        RoomType single = TestData.roomType(roomTypeRepository, "Single-Avl", 1, new BigDecimal("80.00"));
        TestData.room(roomRepository, single, "S-001", 1, RoomStatus.AVAILABLE);

        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(2);
        List<AvailabilityRoomDto> available = availabilityService.search(in, out, 2, null);
        assertTrue(available.stream().allMatch(r -> r.getMaxCapacity() >= 2));
    }

    @Test
    void searchInvalidDatesThrows() {
        LocalDate in = LocalDate.now().plusDays(2);
        LocalDate out = LocalDate.now().plusDays(2);
        assertThrows(com.hotelmanager.web.exception.BusinessException.class,
                () -> availabilityService.search(in, out, 1, null));
    }
}
