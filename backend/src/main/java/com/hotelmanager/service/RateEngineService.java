package com.hotelmanager.service;

import com.hotelmanager.domain.CancellationPolicy;
import com.hotelmanager.domain.DailyRateOverride;
import com.hotelmanager.domain.PromotionRule;
import com.hotelmanager.domain.RatePlan;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.ReservationNightlyRate;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.SeasonalRate;
import com.hotelmanager.domain.TaxOrFee;
import com.hotelmanager.domain.enums.DiscountType;
import com.hotelmanager.domain.enums.TaxAppliesTo;
import com.hotelmanager.domain.enums.TaxType;
import com.hotelmanager.repository.CancellationPolicyRepository;
import com.hotelmanager.repository.DailyRateOverrideRepository;
import com.hotelmanager.repository.PromotionRuleRepository;
import com.hotelmanager.repository.RatePlanRepository;
import com.hotelmanager.repository.ReservationNightlyRateRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.repository.SeasonalRateRepository;
import com.hotelmanager.repository.TaxOrFeeRepository;
import com.hotelmanager.service.rate.QuoteResult;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RateEngineService {

    public static final int BASE_ADULTS = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;

    private final RatePlanRepository ratePlanRepository;
    private final SeasonalRateRepository seasonalRateRepository;
    private final DailyRateOverrideRepository dailyRateOverrideRepository;
    private final PromotionRuleRepository promotionRuleRepository;
    private final TaxOrFeeRepository taxOrFeeRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReservationNightlyRateRepository nightlyRateRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;

    public RateEngineService(RatePlanRepository ratePlanRepository,
                             SeasonalRateRepository seasonalRateRepository,
                             DailyRateOverrideRepository dailyRateOverrideRepository,
                             PromotionRuleRepository promotionRuleRepository,
                             TaxOrFeeRepository taxOrFeeRepository,
                             RoomTypeRepository roomTypeRepository,
                             ReservationNightlyRateRepository nightlyRateRepository,
                             CancellationPolicyRepository cancellationPolicyRepository) {
        this.ratePlanRepository = ratePlanRepository;
        this.seasonalRateRepository = seasonalRateRepository;
        this.dailyRateOverrideRepository = dailyRateOverrideRepository;
        this.promotionRuleRepository = promotionRuleRepository;
        this.taxOrFeeRepository = taxOrFeeRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.nightlyRateRepository = nightlyRateRepository;
        this.cancellationPolicyRepository = cancellationPolicyRepository;
    }

    @Transactional(readOnly = true)
    public QuoteResult calculatePrice(LocalDate checkIn, LocalDate checkOut, Long roomTypeId,
                                      Integer adults, Integer children, Long ratePlanId,
                                      String promotionCode) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "checkOut must be strictly after checkIn");
        }
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Room type not found: " + roomTypeId));
        int adultCount = adults == null ? 0 : adults;
        int childCount = children == null ? 0 : children;

        RatePlan ratePlan = resolveRatePlan(roomTypeId, ratePlanId);
        long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
        if (nights > 730) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "A quote cannot exceed 730 nights");
        }

        List<PromotionRule> applicablePromotions = findApplicablePromotions(ratePlan, checkIn,
                (int) nights, adultCount + childCount, promotionCode);

        BigDecimal adultExtraRate = ratePlan != null ? nz(ratePlan.getAdultExtraRate()) : BigDecimal.ZERO;
        BigDecimal childExtraRate = ratePlan != null ? ncz(ratePlan.getChildExtraRate()) : BigDecimal.ZERO;
        int extraAdults = Math.max(0, adultCount - BASE_ADULTS);
        int extraChildren = childCount;
        BigDecimal extraPersonChargePerNight = adultExtraRate.multiply(BigDecimal.valueOf(extraAdults))
                .add(childExtraRate.multiply(BigDecimal.valueOf(extraChildren)))
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTaxes = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        List<QuoteResult.NightlyBreakdown> breakdown = new ArrayList<>();

        LocalDate night = checkIn;
        int nightIndex = 0;
        while (night.isBefore(checkOut)) {
            BigDecimal baseRate = resolveBaseRate(ratePlan, roomType, night);
            BigDecimal extraPersonCharge = extraPersonChargePerNight;
            BigDecimal roomRate = baseRate.add(extraPersonCharge).setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal discountAmount = computeDiscount(applicablePromotions, roomRate, nights);
            BigDecimal roomRateTaxes = BigDecimal.ZERO;
            BigDecimal perNightFees = BigDecimal.ZERO;
            List<TaxOrFee> taxes = taxOrFeeRepository.findActiveOn(night);
            for (TaxOrFee t : taxes) {
                if (t.getType() == TaxType.TAX_PERCENT
                        && (t.getAppliesTo() == TaxAppliesTo.ROOM_RATE || t.getAppliesTo() == TaxAppliesTo.PER_NIGHT)) {
                    roomRateTaxes = roomRateTaxes.add(roomRate.multiply(nz(t.getValue())).divide(HUNDRED, SCALE, RoundingMode.HALF_UP));
                } else if (t.getType() == TaxType.FEE_FIXED && t.getAppliesTo() == TaxAppliesTo.PER_NIGHT) {
                    perNightFees = perNightFees.add(nz(t.getValue()));
                }
            }
            BigDecimal fees = perNightFees;
            if (nightIndex == 0) {
                for (TaxOrFee t : taxes) {
                    if (t.getType() == TaxType.FEE_FIXED && t.getAppliesTo() == TaxAppliesTo.TOTAL) {
                        fees = fees.add(nz(t.getValue()));
                    }
                }
            }
            BigDecimal taxedBase = roomRate.subtract(discountAmount);
            BigDecimal totalTaxesForNight = roomRateTaxes;
            for (TaxOrFee t : taxes) {
                if (t.getType() == TaxType.TAX_PERCENT && t.getAppliesTo() == TaxAppliesTo.TOTAL) {
                    BigDecimal baseForTotal = taxedBase.add(totalTaxesForNight).add(fees);
                    totalTaxesForNight = totalTaxesForNight.add(
                            baseForTotal.multiply(nz(t.getValue())).divide(HUNDRED, SCALE, RoundingMode.HALF_UP));
                }
            }
            BigDecimal nightTotal = baseRate.add(extraPersonCharge)
                    .subtract(discountAmount)
                    .add(totalTaxesForNight)
                    .add(fees)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            breakdown.add(QuoteResult.night(night, baseRate.setScale(SCALE, RoundingMode.HALF_UP),
                    extraPersonCharge, discountAmount, totalTaxesForNight, fees, nightTotal));

            subtotal = subtotal.add(roomRate);
            totalDiscount = totalDiscount.add(discountAmount);
            totalTaxes = totalTaxes.add(totalTaxesForNight);
            totalFees = totalFees.add(fees);

            night = night.plusDays(1);
            nightIndex++;
        }

        BigDecimal grandTotal = subtotal.subtract(totalDiscount).add(totalTaxes).add(totalFees)
                .setScale(SCALE, RoundingMode.HALF_UP);
        Long resolvedPlanId = ratePlan != null ? ratePlan.getId() : null;
        return new QuoteResult(resolvedPlanId, breakdown,
                subtotal.setScale(SCALE, RoundingMode.HALF_UP),
                totalDiscount.setScale(SCALE, RoundingMode.HALF_UP),
                totalTaxes.setScale(SCALE, RoundingMode.HALF_UP),
                totalFees.setScale(SCALE, RoundingMode.HALF_UP),
                grandTotal);
    }

    @Transactional
    public List<ReservationNightlyRate> createNightlySnapshot(Reservation reservation, Long ratePlanId,
                                                              QuoteResult breakdown) {
        if (breakdown == null || breakdown.getNightly() == null || breakdown.getNightly().isEmpty()) {
            return List.of();
        }
        List<ReservationNightlyRate> rows = new ArrayList<>();
        for (QuoteResult.NightlyBreakdown nb : breakdown.getNightly()) {
            ReservationNightlyRate rate = new ReservationNightlyRate();
            rate.setReservation(reservation);
            rate.setRatePlanId(ratePlanId);
            rate.setNightDate(nb.getDate());
            rate.setBaseRate(nb.getBaseRate());
            rate.setExtraPersonCharge(nb.getExtraPersonCharge());
            rate.setDiscountAmount(nb.getDiscountAmount());
            rate.setTaxesAmount(nb.getTaxesAmount());
            rate.setFeesAmount(nb.getFeesAmount());
            rate.setTotal(nb.getTotal());
            rate.setIncluded(true);
            rows.add(nightlyRateRepository.save(rate));
        }
        return rows;
    }

    @Transactional
    public void recalculateTotal(Reservation reservation) {
        BigDecimal sum = nightlyRateRepository.sumIncludedByReservation(reservation.getId());
        reservation.setTotalAmount(sum.setScale(SCALE, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public Optional<RatePlan> resolveRatePlanOptional(Long roomTypeId, Long ratePlanId) {
        return Optional.ofNullable(resolveRatePlan(roomTypeId, ratePlanId));
    }

    @Transactional(readOnly = true)
    public CancellationPolicy findCancellationPolicy(Long policyId) {
        if (policyId == null) {
            return null;
        }
        return cancellationPolicyRepository.findById(policyId).orElse(null);
    }

    private RatePlan resolveRatePlan(Long roomTypeId, Long ratePlanId) {
        if (ratePlanId != null) {
            RatePlan plan = ratePlanRepository.findById(ratePlanId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RATE_NOT_FOUND,
                            "Rate plan not found: " + ratePlanId));
            if (!Boolean.TRUE.equals(plan.getActive())) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.RATE_NOT_FOUND,
                        "Rate plan is not active: " + ratePlanId);
            }
            return plan;
        }
        return ratePlanRepository.findDefaultByRoomType(roomTypeId).orElse(null);
    }

    private BigDecimal resolveBaseRate(RatePlan ratePlan, RoomType roomType, LocalDate night) {
        if (ratePlan != null) {
            List<DailyRateOverride> overrides =
                    dailyRateOverrideRepository.findForNight(roomType.getId(), night, ratePlan.getId());
            if (!overrides.isEmpty()) {
                return overrides.get(0).getPrice().setScale(SCALE, RoundingMode.HALF_UP);
            }
            List<SeasonalRate> seasons = seasonalRateRepository.findApplicable(ratePlan.getId(), night);
            if (!seasons.isEmpty()) {
                SeasonalRate season = seasons.get(0);
                int idx = night.getDayOfWeek().getValue() - 1;
                BigDecimal factor = safeIndex(season.getWeekdays(), idx, BigDecimal.ONE);
                BigDecimal weekdayRate = safeIndex(ratePlan.getWeekdayRates(), idx, roomType.getBasePrice());
                if (season.getPriceMode() == com.hotelmanager.domain.enums.PriceMode.ABSOLUTE) {
                    return factor.setScale(SCALE, RoundingMode.HALF_UP);
                }
                return weekdayRate.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP);
            }
            int idx = night.getDayOfWeek().getValue() - 1;
            BigDecimal weekdayRate = safeIndex(ratePlan.getWeekdayRates(), idx, roomType.getBasePrice());
            return weekdayRate.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return roomType.getBasePrice().setScale(SCALE, RoundingMode.HALF_UP);
    }

    private List<PromotionRule> findApplicablePromotions(RatePlan ratePlan, LocalDate checkIn,
                                                         int nights, int guests, String promotionCode) {
        Long planId = ratePlan != null ? ratePlan.getId() : null;
        List<PromotionRule> candidates = promotionRuleRepository.findActiveForPlan(planId);
        List<PromotionRule> applicable = new ArrayList<>();
        for (PromotionRule p : candidates) {
            if (p.getValidFrom() != null && checkIn.isBefore(p.getValidFrom())) {
                continue;
            }
            if (p.getValidTo() != null && checkIn.isAfter(p.getValidTo())) {
                continue;
            }
            if (nights < nzInt(p.getMinNights(), 1)) {
                continue;
            }
            if (guests < nzInt(p.getMinGuests(), 1)) {
                continue;
            }
            if (p.getRatePlanId() != null && ratePlan != null
                    && !p.getRatePlanId().equals(ratePlan.getId())) {
                continue;
            }
            if (promotionCode != null && !promotionCode.isBlank()
                    && !promotionCode.equalsIgnoreCase(p.getCode())) {
                continue;
            }
            applicable.add(p);
        }
        return applicable;
    }

    private BigDecimal computeDiscount(List<PromotionRule> promotions, BigDecimal roomRate, long nights) {
        if (promotions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        boolean anyStackable = promotions.stream().anyMatch(p -> Boolean.TRUE.equals(p.getStackable()));
        if (!anyStackable) {
            PromotionRule chosen = promotions.stream()
                    .sorted(Comparator.comparing(PromotionRule::getPriority, Comparator.reverseOrder())
                            .thenComparing(PromotionRule::getCode))
                    .findFirst().orElseThrow();
            return discountForRule(chosen, roomRate, nights);
        }
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal cap = roomRate;
        for (PromotionRule p : promotions) {
            BigDecimal d = discountForRule(p, roomRate, nights);
            if (totalDiscount.add(d).compareTo(cap) > 0) {
                d = cap.subtract(totalDiscount);
            }
            totalDiscount = totalDiscount.add(d);
            if (totalDiscount.compareTo(cap) >= 0) {
                break;
            }
        }
        return totalDiscount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal discountForRule(PromotionRule p, BigDecimal roomRate, long nights) {
        BigDecimal value = nz(p.getDiscountValue());
        BigDecimal amount;
        if (p.getDiscountType() == DiscountType.PERCENTAGE) {
            amount = roomRate.multiply(value).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        } else {
            amount = value.multiply(BigDecimal.valueOf(nights)).setScale(SCALE, RoundingMode.HALF_UP);
        }
        if (amount.compareTo(roomRate) > 0) {
            amount = roomRate;
        }
        return amount;
    }

    private BigDecimal safeIndex(List<BigDecimal> list, int idx, BigDecimal fallback) {
        if (list == null || idx < 0 || idx >= list.size()) {
            return ncz(fallback);
        }
        return ncz(list.get(idx));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal ncz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static int nzInt(Integer v, int fallback) {
        return v == null ? fallback : v;
    }
}
