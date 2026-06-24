package com.hotelmanager.service.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class QuoteResult {

    private final Long ratePlanId;
    private final List<NightlyBreakdown> nightly;
    private final BigDecimal subtotal;
    private final BigDecimal totalDiscount;
    private final BigDecimal totalTaxes;
    private final BigDecimal totalFees;
    private final BigDecimal grandTotal;

    public QuoteResult(Long ratePlanId, List<NightlyBreakdown> nightly,
                       BigDecimal subtotal, BigDecimal totalDiscount,
                       BigDecimal totalTaxes, BigDecimal totalFees, BigDecimal grandTotal) {
        this.ratePlanId = ratePlanId;
        this.nightly = nightly;
        this.subtotal = subtotal;
        this.totalDiscount = totalDiscount;
        this.totalTaxes = totalTaxes;
        this.totalFees = totalFees;
        this.grandTotal = grandTotal;
    }

    public Long getRatePlanId() {
        return ratePlanId;
    }

    public List<NightlyBreakdown> getNightly() {
        return nightly;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public BigDecimal getTotalTaxes() {
        return totalTaxes;
    }

    public BigDecimal getTotalFees() {
        return totalFees;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public static NightlyBreakdown night(LocalDate date, BigDecimal baseRate, BigDecimal extraPersonCharge,
                                         BigDecimal discountAmount, BigDecimal taxesAmount,
                                         BigDecimal feesAmount, BigDecimal total) {
        return new NightlyBreakdown(date, baseRate, extraPersonCharge, discountAmount, taxesAmount, feesAmount, total);
    }

    public static final class NightlyBreakdown {
        private final LocalDate date;
        private final BigDecimal baseRate;
        private final BigDecimal extraPersonCharge;
        private final BigDecimal discountAmount;
        private final BigDecimal taxesAmount;
        private final BigDecimal feesAmount;
        private final BigDecimal total;

        public NightlyBreakdown(LocalDate date, BigDecimal baseRate, BigDecimal extraPersonCharge,
                                BigDecimal discountAmount, BigDecimal taxesAmount,
                                BigDecimal feesAmount, BigDecimal total) {
            this.date = date;
            this.baseRate = baseRate;
            this.extraPersonCharge = extraPersonCharge;
            this.discountAmount = discountAmount;
            this.taxesAmount = taxesAmount;
            this.feesAmount = feesAmount;
            this.total = total;
        }

        public LocalDate getDate() {
            return date;
        }

        public BigDecimal getBaseRate() {
            return baseRate;
        }

        public BigDecimal getExtraPersonCharge() {
            return extraPersonCharge;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public BigDecimal getTaxesAmount() {
            return taxesAmount;
        }

        public BigDecimal getFeesAmount() {
            return feesAmount;
        }

        public BigDecimal getTotal() {
            return total;
        }
    }
}
