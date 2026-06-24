package com.hotelmanager;

import com.hotelmanager.config.BusinessClock;
import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.RatePlan;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.User;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.domain.enums.UserRole;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.RatePlanRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.repository.UserRepository;
import com.hotelmanager.security.RefreshTokenService;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de concurrencia (ER-23) contra PostgreSQL real (Testcontainers).
 *
 * <p>Sin {@code @Transactional} a nivel de clase: los hilos lanzados deben
 * ejecutarse en transacciones independientes y ver los datos confirmados por
 * el hilo de setup. Los identificadores (numero de habitacion, documento,
 * email) son unicos por prueba para evitar colisiones entre pruebas que
 * comparten el contenedor estatico de la clase.</p>
 */
class ConcurrencyTest extends PostgresIntegrationTest {

    private static final BigDecimal BASE_RATE = new BigDecimal("1500.00");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ReservationService reservationService;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private BusinessClock clock;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private GuestRepository guestRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private RatePlanRepository ratePlanRepository;
    @Autowired private UserRepository userRepository;

    private static int seq() {
        return SEQ.incrementAndGet();
    }

    private LocalDate today() {
        return clock.today();
    }

    private RoomType newRoomType() {
        return TestData.roomType(roomTypeRepository, "CNC-CONC-RT-" + seq(), 4, BASE_RATE);
    }

    private Room newRoom(RoomType rt) {
        return TestData.room(roomRepository, rt, "CONC-" + seq(), 1, RoomStatus.AVAILABLE);
    }

    private Guest newGuest() {
        return TestData.guest(guestRepository, "DOC-CONC-" + seq());
    }

    private void defaultPlan(RoomType rt) {
        RatePlan rp = new RatePlan();
        rp.setCode("CONC-" + seq());
        rp.setName("Concurrency Test Plan");
        rp.setRoomType(rt);
        rp.setWeekdayRates(new ArrayList<>(Collections.nCopies(7, BASE_RATE)));
        rp.setAdultExtraRate(BigDecimal.ZERO);
        rp.setChildExtraRate(BigDecimal.ZERO);
        rp.setMinNights(1);
        rp.setIsDefault(true);
        rp.setActive(true);
        rp.setValidFrom(today());
        ratePlanRepository.save(rp);
    }

    private ReservationCreateRequest createReq(Guest guest, RoomType rt, Room room,
                                                LocalDate in, LocalDate out) {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setGuestId(guest.getId());
        req.setCheckIn(in);
        req.setCheckOut(out);
        req.setAdults(2);
        req.setChildren(0);
        req.setRoomTypeId(rt.getId());
        req.setRoomId(room.getId());
        return req;
    }

    @Test
    void twoConcurrentReservationsForSameRoomYieldOneSuccessAndOneOverlap() throws Exception {
        RoomType rt = newRoomType();
        Room room = newRoom(rt);
        defaultPlan(rt);
        Guest guest = newGuest();
        LocalDate in = today().plusDays(100);
        LocalDate out = today().plusDays(102);

        List<Attempt<ReservationDto>> attempts =
                runConcurrently(2, () -> reservationService.create(createReq(guest, rt, room, in, out)));

        long successes = attempts.stream().filter(Attempt::isOk).count();
        long overlapErrors = attempts.stream()
                .filter(a -> a.error() instanceof BusinessException
                        && ((BusinessException) a.error()).getCode() == ErrorCode.RESERVATION_OVERLAP)
                .count();
        assertEquals(1, successes, "exactly one reservation must succeed");
        assertEquals(1, overlapErrors, "the other must be rejected with RESERVATION_OVERLAP (GiST + room lock)");
    }

    @Test
    void twoConcurrentCheckInsForSameReservationYieldOneSuccessAndOne409() throws Exception {
        RoomType rt = newRoomType();
        newRoom(rt);
        defaultPlan(rt);
        Guest guest = newGuest();
        LocalDate in = today();
        LocalDate out = today().plusDays(2);
        Reservation reservation = TestData.reservation(reservationRepository, guest, rt,
                ReservationStatus.CONFIRMED, in, out, 2, 0, null);
        Long resId = reservation.getId();

        List<Attempt<ReservationDto>> attempts =
                runConcurrently(2, () -> reservationService.checkIn(resId, null));

        long successes = attempts.stream().filter(Attempt::isOk).count();
        long errors409 = attempts.stream()
                .filter(a -> a.error() instanceof BusinessException
                        && ((BusinessException) a.error()).getStatus().value() == 409)
                .count();
        assertEquals(1, successes, "exactly one check-in must succeed");
        assertEquals(1, errors409, "the other must be rejected with a 409 (DUPLICATE_CHECKIN or RESERVATION_OVERLAP)");
        assertEquals(ReservationStatus.CHECKED_IN,
                reservationRepository.findById(resId).orElseThrow().getStatus());
    }

    @Test
    void twoConcurrentRefreshTokenRotationsKeepChainConsistent() throws Exception {
        User user = TestData.user(userRepository, "conc-rt-" + seq() + "@hotel.test", UserRole.ADMIN);
        String raw = refreshTokenService.generate(user.getId(), "concurrency-test");

        List<Attempt<RefreshTokenService.RotationResult>> attempts =
                runConcurrently(2, () -> refreshTokenService.rotate(raw));

        long successes = attempts.stream().filter(Attempt::isOk).count();
        long authErrors = attempts.stream()
                .filter(a -> a.error() instanceof BusinessException
                        && ((BusinessException) a.error()).getStatus().value() == 401)
                .count();
        assertTrue(successes >= 1, "at least one rotation must succeed");
        assertEquals(2, successes + authErrors,
                "both rotations must terminate as success or 401 reuse detection");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.validate(raw));
        assertEquals(401, ex.getStatus().value(),
                "the original token must be revoked after the concurrent rotations");
    }

    private <T> List<Attempt<T>> runConcurrently(int n, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                return task.call();
            }));
        }
        ready.await();
        start.countDown();
        List<Attempt<T>> results = new ArrayList<>();
        for (Future<T> f : futures) {
            try {
                results.add(Attempt.ok(f.get(30, TimeUnit.SECONDS)));
            } catch (ExecutionException ee) {
                results.add(Attempt.err(ee.getCause() != null ? ee.getCause() : ee));
            }
        }
        pool.shutdownNow();
        return results;
    }

    private static final class Attempt<T> {
        private final T value;
        private final Throwable error;

        private Attempt(T value, Throwable error) {
            this.value = value;
            this.error = error;
        }

        static <T> Attempt<T> ok(T value) {
            return new Attempt<>(value, null);
        }

        static <T> Attempt<T> err(Throwable t) {
            return new Attempt<>(null, t);
        }

        boolean isOk() {
            return error == null;
        }

        T value() {
            return value;
        }

        Throwable error() {
            return error;
        }
    }
}
