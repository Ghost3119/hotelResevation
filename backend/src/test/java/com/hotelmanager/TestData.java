package com.hotelmanager;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.User;
import com.hotelmanager.domain.enums.PaymentMethod;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.domain.enums.UserRole;
import com.hotelmanager.domain.Payment;
import com.hotelmanager.domain.enums.PaymentStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TestData {

    private TestData() {
    }

    public static User user(UserRepository repo, String email, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setFullName(role == UserRole.ADMIN ? "Admin Test" : "Recep Test");
        u.setRole(role);
        u.setActive(true);
        return repo.save(u);
    }

    public static User userWithPassword(UserRepository repo, PasswordEncoder encoder, String email, String plain, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(plain));
        u.setFullName(role == UserRole.ADMIN ? "Admin Test" : "Recep Test");
        u.setRole(role);
        u.setActive(true);
        return repo.save(u);
    }

    public static RoomType roomType(RoomTypeRepository repo, String name, int capacity, BigDecimal price) {
        RoomType rt = new RoomType();
        rt.setName(name);
        rt.setDescription("desc");
        rt.setMaxCapacity(capacity);
        rt.setBasePrice(price);
        rt.setAmenities(java.util.List.of("wifi"));
        rt.setActive(true);
        return repo.save(rt);
    }

    public static Room room(RoomRepository repo, RoomType rt, String number, int floor, RoomStatus status) {
        Room r = new Room();
        r.setNumber(number);
        r.setFloor(floor);
        r.setRoomType(rt);
        r.setStatus(status);
        return repo.save(r);
    }

    public static Guest guest(GuestRepository repo, String doc) {
        Guest g = new Guest();
        g.setFirstName("Juan");
        g.setLastName("Perez");
        g.setEmail("juan@example.com");
        g.setPhone("+34600000000");
        g.setDocumentNumber(doc);
        g.setNationality("Espana");
        return repo.save(g);
    }

    public static Reservation reservation(ReservationRepository repo, Guest guest, RoomType rt,
                                          ReservationStatus status, LocalDate checkIn, LocalDate checkOut,
                                          int adults, int children, Long createdBy) {
        Reservation r = new Reservation();
        r.setGuest(guest);
        r.setStatus(status);
        r.setCheckIn(checkIn);
        r.setCheckOut(checkOut);
        r.setAdults(adults);
        r.setChildren(children);
        r.setRoomType(rt);
        r.setNightlyPrice(rt.getBasePrice());
        long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
        r.setTotalAmount(rt.getBasePrice().multiply(BigDecimal.valueOf(nights)));
        r.setCreatedBy(createdBy);
        return repo.save(r);
    }

    public static Payment payment(PaymentRepository repo, Reservation r, BigDecimal amount,
                                  PaymentMethod method, PaymentStatus status) {
        Payment p = new Payment();
        p.setReservation(r);
        p.setAmount(amount);
        p.setMethod(method);
        p.setStatus(status);
        p.setReference("REF");
        if (status == PaymentStatus.COMPLETED) {
            p.setPaidAt(java.time.Instant.now());
        }
        return repo.save(p);
    }
}
