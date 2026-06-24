package com.hotelmanager;

import com.hotelmanager.domain.CancellationPolicy;
import com.hotelmanager.domain.DailyRateOverride;
import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.PromotionRule;
import com.hotelmanager.domain.RatePlan;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.ReservationNightlyRate;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.SeasonalRate;
import com.hotelmanager.domain.TaxOrFee;
import com.hotelmanager.domain.enums.DiscountType;
import com.hotelmanager.domain.enums.PriceMode;
import com.hotelmanager.domain.enums.PenaltyType;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.domain.enums.SeasonType;
import com.hotelmanager.domain.enums.TaxAppliesTo;
import com.hotelmanager.domain.enums.TaxType;
import com.hotelmanager.repository.CancellationPolicyRepository;
import com.hotelmanager.repository.DailyRateOverrideRepository;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.PromotionRuleRepository;
import com.hotelmanager.repository.RatePlanRepository;
import com.hotelmanager.repository.ReservationNightlyRateRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.repository.SeasonalRateRepository;
import com.hotelmanager.repository.TaxOrFeeRepository;
import com.hotelmanager.service.RateEngineService;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.service.SeasonalRateService;
import com.hotelmanager.service.TaxOrFeeService;
import com.hotelmanager.service.PromotionRuleService;
import com.hotelmanager.service.DailyRateOverrideService;
import com.hotelmanager.service.rate.QuoteResult;
import com.hotelmanager.web.dto.DailyRateOverrideCreateRequest;
import com.hotelmanager.web.dto.PromotionRuleCreateRequest;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.dto.SeasonalRateCreateRequest;
import com.hotelmanager.web.dto.TaxOrFeeCreateRequest;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion del motor tarifario contra PostgreSQL real
 * (Testcontainers). Valida ER-1..ER-8 y ER-9 (snapshot por noche).
 */
@Transactional
class RateEngineIntegrationTest extends PostgresIntegrationTest {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal BASE_RATE = new BigDecimal("1500.00");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private RateEngineService rateEngineService;
    @Autowired private ReservationService reservationService;
    @Autowired private SeasonalRateService seasonalRateService;
    @Autowired private TaxOrFeeService taxOrFeeService;
    @Autowired private PromotionRuleService promotionRuleService;
    @Autowired private DailyRateOverrideService dailyRateOverrideService;
    @Autowired private RatePlanRepository ratePlanRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private GuestRepository guestRepository;
    @Autowired private ReservationNightlyRateRepository nightlyRateRepository;
    @Autowired private ReservationRoomRepository reservationRoomRepository;
    @Autowired private TaxOrFeeRepository taxOrFeeRepository;
    @Autowired private PromotionRuleRepository promotionRuleRepository;
    @Autowired private SeasonalRateRepository seasonalRateRepository;
    @Autowired private DailyRateOverrideRepository dailyRateOverrideRepository;
    @Autowired private CancellationPolicyRepository cancellationPolicyRepository;
    @Autowired private PaymentRepository paymentRepository;

    private RoomType roomType;
    private Room room;
    private Guest guest;
    private RatePlan plan;

    @BeforeEach
    void setup() {
        roomType = TestData.roomType(roomTypeRepository, "RTE-RT-" + seq(), 4, BASE_RATE);
        room = TestData.room(roomRepository, roomType, "RTE-" + seq(), 1, RoomStatus.AVAILABLE);
        guest = TestData.guest(guestRepository, "DOC-RTE-" + seq());
        plan = defaultPlan(roomType, BASE_RATE, null);
    }

    private static int seq() {
        return SEQ.incrementAndGet();
    }

    private RatePlan defaultPlan(RoomType rt, BigDecimal rate, Long policyId) {
        RatePlan rp = new RatePlan();
        rp.setCode("RTE-" + seq());
        rp.setName("Rate Engine Test Plan");
        rp.setRoomType(rt);
        rp.setWeekdayRates(new ArrayList<>(Collections.nCopies(7, rate)));
        rp.setAdultExtraRate(BigDecimal.ZERO);
        rp.setChildExtraRate(BigDecimal.ZERO);
        rp.setCancellationPolicyId(policyId);
        rp.setMinNights(1);
        rp.setIsDefault(true);
        rp.setActive(true);
        rp.setValidFrom(LocalDate.now());
        return ratePlanRepository.save(rp);
    }

    private BigDecimal seededRoomRateTaxPercent(LocalDate night) {
        return taxOrFeeRepository.findActiveOn(night).stream()
                .filter(t -> t.getType() == TaxType.TAX_PERCENT
                        && t.getAppliesTo() == TaxAppliesTo.ROOM_RATE)
                .map(TaxOrFee::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ReservationCreateRequest createReq(LocalDate in, LocalDate out, Integer adults) {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setGuestId(guest.getId());
        req.setCheckIn(in);
        req.setCheckOut(out);
        req.setAdults(adults == null ? 2 : adults);
        req.setChildren(0);
        req.setRoomTypeId(roomType.getId());
        req.setRoomId(room.getId());
        return req;
    }

    @Test
    void nightlySnapshotCreatedOnConfirmationWithCorrectAmounts() {
        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        ReservationDto dto = reservationService.create(createReq(in, out, 2));

        assertEquals(com.hotelmanager.domain.enums.ReservationStatus.CONFIRMED, dto.getStatus());
        List<ReservationNightlyRate> rows =
                nightlyRateRepository.findByReservationIdOrderByNightDateAsc(dto.getId());
        assertEquals(2, rows.size());
        assertEquals(in, rows.get(0).getNightDate());
        assertEquals(in.plusDays(1), rows.get(1).getNightDate());
        for (ReservationNightlyRate n : rows) {
            assertTrue(Boolean.TRUE.equals(n.getIncluded()));
            assertEquals(BASE_RATE, n.getBaseRate().setScale(2, RoundingMode.HALF_UP));
            assertEquals(new BigDecimal("0.00"), n.getExtraPersonCharge().setScale(2, RoundingMode.HALF_UP));
            assertEquals(new BigDecimal("0.00"), n.getDiscountAmount().setScale(2, RoundingMode.HALF_UP));
            assertEquals(new BigDecimal("0.00"), n.getFeesAmount().setScale(2, RoundingMode.HALF_UP));
            assertTrue(n.getTaxesAmount().compareTo(BigDecimal.ZERO) > 0,
                    "seeded IVA+ISH taxes must be applied on real PostgreSQL");
            BigDecimal expectedTotal = n.getBaseRate().add(n.getTaxesAmount());
            assertEquals(expectedTotal.setScale(2, RoundingMode.HALF_UP), n.getTotal().setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal sumIncluded = nightlyRateRepository.sumIncludedByReservation(dto.getId());
        assertEquals(sumIncluded.setScale(2, RoundingMode.HALF_UP),
                dto.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void overlappingSeasonalRatesRejectedWithSeasonOverlap() {
        SeasonalRateCreateRequest first = seasonReq(plan.getId(), "Alta-Test",
                LocalDate.now().plusDays(20), LocalDate.now().plusDays(30));
        seasonalRateService.create(first);

        SeasonalRateCreateRequest overlap = seasonReq(plan.getId(), "Overlap-Test",
                LocalDate.now().plusDays(25), LocalDate.now().plusDays(35));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> seasonalRateService.create(overlap));
        assertEquals(ErrorCode.SEASON_OVERLAP, ex.getCode());
        assertEquals(1, seasonalRateRepository.findByRatePlanId(plan.getId()).size());
    }

    @Test
    void taxWithFutureValidFromNotAppliedToTodayQuote() {
        TaxOrFeeCreateRequest futureTax = new TaxOrFeeCreateRequest();
        futureTax.setName("FUTURE-TAX-" + seq());
        futureTax.setType(TaxType.TAX_PERCENT);
        futureTax.setValue(new BigDecimal("10.00"));
        futureTax.setAppliesTo(TaxAppliesTo.ROOM_RATE);
        futureTax.setValidFrom(LocalDate.now().plusDays(1));
        futureTax.setActive(true);
        taxOrFeeService.create(futureTax);

        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        QuoteResult quote = rateEngineService.calculatePrice(in, out,
                roomType.getId(), 2, 0, plan.getId(), null);

        assertEquals(2, quote.getNightly().size());
        BigDecimal seededRatePct = seededRoomRateTaxPercent(in);
        BigDecimal expectedTaxes = BASE_RATE.multiply(seededRatePct)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        for (QuoteResult.NightlyBreakdown nb : quote.getNightly()) {
            assertEquals(expectedTaxes, nb.getTaxesAmount().setScale(2, RoundingMode.HALF_UP),
                    "future-vigency tax (10%) must NOT be applied to current nights");
        }
    }

    @Test
    void nonStackablePromotionsSamePriorityApplyOnlyOne() {
        promotionRuleService.create(promo("AAA", new BigDecimal("10.00"), 0));
        promotionRuleService.create(promo("BBB", new BigDecimal("10.00"), 0));

        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        QuoteResult quote = rateEngineService.calculatePrice(in, out,
                roomType.getId(), 2, 0, plan.getId(), null);

        BigDecimal roomRate = BASE_RATE;
        BigDecimal expectedDiscountPerNight = roomRate.multiply(new BigDecimal("10.00"))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal expectedTotalDiscount = expectedDiscountPerNight.multiply(BigDecimal.valueOf(2));
        assertEquals(expectedTotalDiscount, quote.getTotalDiscount().setScale(2, RoundingMode.HALF_UP),
                "only one non-stackable promotion must apply (not 20%)");
        for (QuoteResult.NightlyBreakdown nb : quote.getNightly()) {
            assertEquals(expectedDiscountPerNight, nb.getDiscountAmount().setScale(2, RoundingMode.HALF_UP));
        }
    }

    @Test
    void dailyRateOverrideTakesPrecedenceOverSeasonalRate() {
        seasonalRateService.create(seasonReq(plan.getId(), "Season-1.5x",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(10),
                new BigDecimal("1.500")));

        LocalDate overrideDate = LocalDate.now().plusDays(2);
        DailyRateOverrideCreateRequest ov = new DailyRateOverrideCreateRequest();
        ov.setRoomTypeId(roomType.getId());
        ov.setRatePlanId(plan.getId());
        ov.setDate(overrideDate);
        ov.setPrice(new BigDecimal("999.00"));
        ov.setReason("Override test");
        dailyRateOverrideService.create(ov);

        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(4);
        QuoteResult quote = rateEngineService.calculatePrice(in, out,
                roomType.getId(), 2, 0, plan.getId(), null);

        assertEquals(3, quote.getNightly().size());
        BigDecimal seasonalRate = BASE_RATE.multiply(new BigDecimal("1.500"))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(seasonalRate, quote.getNightly().get(0).getBaseRate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("999.00"), quote.getNightly().get(1).getBaseRate().setScale(2, RoundingMode.HALF_UP));
        assertEquals(seasonalRate, quote.getNightly().get(2).getBaseRate().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void extraPersonChargeAppliedWhenAdultsExceedTwo() {
        plan.setAdultExtraRate(new BigDecimal("100.00"));
        ratePlanRepository.save(plan);

        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        QuoteResult quote = rateEngineService.calculatePrice(in, out,
                roomType.getId(), 4, 0, plan.getId(), null);

        BigDecimal expectedExtra = new BigDecimal("100.00").multiply(BigDecimal.valueOf(2));
        for (QuoteResult.NightlyBreakdown nb : quote.getNightly()) {
            assertEquals(expectedExtra.setScale(2, RoundingMode.HALF_UP),
                    nb.getExtraPersonCharge().setScale(2, RoundingMode.HALF_UP));
            assertEquals(BASE_RATE, nb.getBaseRate().setScale(2, RoundingMode.HALF_UP));
        }
    }

    private SeasonalRateCreateRequest seasonReq(Long planId, String name,
                                                 LocalDate start, LocalDate end) {
        return seasonReq(planId, name, start, end, new BigDecimal("1.250"));
    }

    private SeasonalRateCreateRequest seasonReq(Long planId, String name,
                                                 LocalDate start, LocalDate end,
                                                 BigDecimal multiplier) {
        SeasonalRateCreateRequest req = new SeasonalRateCreateRequest();
        req.setRatePlanId(planId);
        req.setName(name);
        req.setStartDate(start);
        req.setEndDate(end);
        req.setSeasonType(SeasonType.ALTA);
        req.setPriceMode(PriceMode.MULTIPLIER);
        req.setWeekdays(new ArrayList<>(Collections.nCopies(7, multiplier)));
        return req;
    }

    private PromotionRuleCreateRequest promo(String code, BigDecimal pct, int priority) {
        PromotionRuleCreateRequest req = new PromotionRuleCreateRequest();
        req.setCode(code + "-" + seq());
        req.setDescription("Test promo " + code);
        req.setDiscountType(DiscountType.PERCENTAGE);
        req.setDiscountValue(pct);
        req.setMinNights(1);
        req.setMinGuests(1);
        req.setValidFrom(LocalDate.now());
        req.setRatePlanId(null);
        req.setStackable(false);
        req.setPriority(priority);
        req.setActive(true);
        return req;
    }
}
