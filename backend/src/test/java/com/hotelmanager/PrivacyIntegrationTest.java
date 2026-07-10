package com.hotelmanager;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.PersonalDataAccessLog;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.DataAccessAction;
import com.hotelmanager.domain.enums.PaymentMethod;
import com.hotelmanager.domain.enums.PaymentStatus;
import com.hotelmanager.domain.enums.PrivacyRequestStatus;
import com.hotelmanager.domain.enums.PrivacyRequestType;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PersonalDataAccessLogRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.service.GuestService;
import com.hotelmanager.service.PrivacyService;
import com.hotelmanager.web.dto.GuestDto;
import com.hotelmanager.web.dto.GuestFullDto;
import com.hotelmanager.web.dto.GuestFullExportDto;
import com.hotelmanager.web.dto.PrivacyRequestCreateRequest;
import com.hotelmanager.web.dto.PrivacyRequestDto;
import com.hotelmanager.web.mapper.GuestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion de privacidad (ER-24..ER-26) contra PostgreSQL real
 * (Testcontainers): exportacion, anonimizacion, enmascaramiento y auditoria de
 * acceso a datos personales.
 */
@Transactional
class PrivacyIntegrationTest extends PostgresIntegrationTest {

    private static final BigDecimal BASE_RATE = new BigDecimal("1500.00");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private PrivacyService privacyService;
    @Autowired private GuestService guestService;
    @Autowired private GuestRepository guestRepository;
    @Autowired private PersonalDataAccessLogRepository accessLogRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    private RoomType roomType;

    private static int seq() {
        return SEQ.incrementAndGet();
    }

    @BeforeEach
    void setup() {
        roomType = TestData.roomType(roomTypeRepository, "PRV-RT-" + seq(), 4, BASE_RATE);
    }

    private Guest guestWith(String doc) {
        return TestData.guest(guestRepository, doc);
    }

    private Reservation reservationFor(Guest g) {
        return TestData.reservation(reservationRepository, g, roomType,
                ReservationStatus.CONFIRMED, LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3), 2, 0, null);
    }

    @Test
    void exportProducesGuestDataAndAuditsExport() {
        Guest g = guestWith("X1234567Z");
        Reservation r = reservationFor(g);
        TestData.payment(paymentRepository, r, new BigDecimal("100.00"),
                PaymentMethod.CASH, PaymentStatus.COMPLETED);

        PrivacyRequestCreateRequest req = new PrivacyRequestCreateRequest();
        req.setGuestId(g.getId());
        req.setType(PrivacyRequestType.EXPORT);
        req.setNotes("subject access request");
        PrivacyRequestDto request = privacyService.create(req);

        GuestFullExportDto export = privacyService.export(request.getId());

        assertNotNull(export.getGuest());
        assertEquals("X1234567Z", export.getGuest().getDocumentNumber(),
                "exported document must be the full (unmasked) value");
        assertFalse(export.getReservations().isEmpty(), "export must include the guest reservations");
        assertFalse(export.getPayments().isEmpty(), "export must include the guest payments");

        PrivacyRequestDto updated = privacyService.get(request.getId());
        assertEquals(PrivacyRequestStatus.COMPLETED, updated.getStatus());

        List<PersonalDataAccessLog> logs = accessLogRepository.findByGuestIdOrderByCreatedAtDesc(g.getId());
        assertTrue(logs.stream().anyMatch(l -> l.getAction() == DataAccessAction.EXPORT),
                "an EXPORT access log entry must be created");
    }

    @Test
    void anonymizeNullsGuestDataButPreservesReservations() {
        Guest g = guestWith("Y9876543W");
        Reservation r = reservationFor(g);
        BigDecimal totalBefore = r.getTotalAmount();

        PrivacyRequestCreateRequest req = new PrivacyRequestCreateRequest();
        req.setGuestId(g.getId());
        req.setType(PrivacyRequestType.DELETE);
        req.setNotes("right to erasure");
        PrivacyRequestDto request = privacyService.create(req);

        PrivacyRequestDto result = privacyService.anonymize(request.getId());
        assertEquals(PrivacyRequestStatus.COMPLETED, result.getStatus());

        Guest anonymized = guestRepository.findById(g.getId()).orElseThrow();
        assertEquals("ANONIMIZADO", anonymized.getFirstName());
        assertEquals("", anonymized.getLastName());
        assertNull(anonymized.getEmail());
        assertNull(anonymized.getPhone());
        assertTrue(anonymized.getDocumentNumber().startsWith("ANONYMIZED-"));
        assertNull(anonymized.getNationality());

        List<Reservation> reservations = reservationRepository.findByGuestIdOrderByCreatedAtDesc(g.getId());
        assertEquals(1, reservations.size(), "reservations must be preserved for financial integrity");
        assertEquals(totalBefore, reservations.get(0).getTotalAmount());

        List<PersonalDataAccessLog> logs = accessLogRepository.findByGuestIdOrderByCreatedAtDesc(g.getId());
        assertTrue(logs.stream().anyMatch(l -> l.getAction() == DataAccessAction.ANONYMIZE));
    }

    @Test
    void documentNumberMaskedForRegularAccessButFullForPrivacyOfficer() {
        String raw = "X1234567Z";
        Guest g = guestWith(raw);

        GuestDto masked = guestService.get(g.getId());
        assertEquals(GuestMapper.maskDocument(raw), masked.getDocumentNumber(),
                "regular guest access must return a masked document number");
        assertNotEquals(raw, masked.getDocumentNumber());

        GuestFullDto full = privacyService.getGuestFull(g.getId(), "privacy officer review");
        assertEquals(raw, full.getDocumentNumber(),
                "privacy officer full access must return the unmasked document number");
    }

    @Test
    void viewingFullDataCreatesViewAccessLog() {
        Guest g = guestWith("Z5555555A");

        privacyService.getGuestFull(g.getId(), "audit justification");

        List<PersonalDataAccessLog> logs = accessLogRepository.findByGuestIdOrderByCreatedAtDesc(g.getId());
        assertTrue(logs.stream().anyMatch(l -> l.getAction() == DataAccessAction.VIEW
                && "audit justification".equals(l.getJustification())),
                "a VIEW access log with the justification must be created on full data read");
    }
}
