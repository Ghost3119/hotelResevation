package com.hotelmanager;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Payment;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.PaymentMethod;
import com.hotelmanager.domain.enums.PaymentStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.service.PaymentService;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.web.dto.PaymentCreateRequest;
import com.hotelmanager.web.dto.PaymentDto;
import com.hotelmanager.web.dto.PaymentStatusRequest;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    private RoomType doble;
    private Guest guest;
    private Long reservationId;

    @BeforeEach
    void setup() {
        doble = TestData.roomType(roomTypeRepository, "Doble-Pay", 2, new BigDecimal("120.00"));
        guest = TestData.guest(guestRepository, "DOC-PAY-1");

        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setGuestId(guest.getId());
        req.setCheckIn(LocalDate.now().plusDays(1));
        req.setCheckOut(LocalDate.now().plusDays(3));
        req.setAdults(2);
        req.setChildren(0);
        req.setRoomTypeId(doble.getId());
        req.setRoomId(null);
        reservationId = reservationService.create(req).getId();
    }

    private PaymentCreateRequest payReq(BigDecimal amount, PaymentMethod method) {
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setAmount(amount);
        req.setMethod(method);
        req.setReference("REF-" + System.nanoTime());
        return req;
    }

    @Test
    void registerPaymentUpdatesPaidAndBalance() {
        PaymentDto p = paymentService.register(reservationId, payReq(new BigDecimal("100.00"), PaymentMethod.CASH));
        assertEquals(PaymentStatus.COMPLETED, p.getStatus());
        assertNotNull(p.getPaidAt());

        ReservationDto res = reservationService.get(reservationId);
        assertEquals(new BigDecimal("240.00"), res.getTotalAmount().setScale(2));
        assertEquals(new BigDecimal("100.00"), res.getPaidAmount().setScale(2));
        assertEquals(new BigDecimal("140.00"), res.getBalance().setScale(2));
    }

    @Test
    void fullPaymentZeroBalance() {
        paymentService.register(reservationId, payReq(new BigDecimal("240.00"), PaymentMethod.CREDIT_CARD));
        ReservationDto res = reservationService.get(reservationId);
        assertEquals(new BigDecimal("240.00"), res.getPaidAmount().setScale(2));
        assertEquals(new BigDecimal("0.00"), res.getBalance().setScale(2));
    }

    @Test
    void refundReducesPaid() {
        PaymentDto p1 = paymentService.register(reservationId, payReq(new BigDecimal("100.00"), PaymentMethod.CASH));
        paymentService.register(reservationId, payReq(new BigDecimal("140.00"), PaymentMethod.CASH));
        assertEquals(new BigDecimal("240.00"), paymentService.sumCompleted(reservationId).setScale(2));

        paymentService.updateStatus(p1.getId(), new PaymentStatusRequest(PaymentStatus.REFUNDED));

        assertEquals(new BigDecimal("140.00"), paymentService.sumCompleted(reservationId).setScale(2));
        ReservationDto res = reservationService.get(reservationId);
        assertEquals(new BigDecimal("140.00"), res.getPaidAmount().setScale(2));
        assertEquals(new BigDecimal("100.00"), res.getBalance().setScale(2));
    }

    @Test
    void registerZeroAmountThrows() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.register(reservationId, payReq(BigDecimal.ZERO, PaymentMethod.CASH)));
        assertNotNull(ex.getCode());
    }

    @Test
    void invalidPaymentTransitionThrows() {
        PaymentDto p = paymentService.register(reservationId, payReq(new BigDecimal("50.00"), PaymentMethod.CASH));
        paymentService.updateStatus(p.getId(), new PaymentStatusRequest(PaymentStatus.REFUNDED));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.updateStatus(p.getId(), new PaymentStatusRequest(PaymentStatus.COMPLETED)));
        assertEquals(ErrorCode.INVALID_STATE_TRANSITION, ex.getCode());
    }
}
