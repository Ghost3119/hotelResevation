package com.hotelmanager.service;

import com.hotelmanager.service.rate.QuoteResult;
import com.hotelmanager.web.dto.QuoteNightlyRateDto;
import com.hotelmanager.web.dto.QuoteRequest;
import com.hotelmanager.web.dto.QuoteResultDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class QuoteService {

    private final RateEngineService rateEngineService;

    public QuoteService(RateEngineService rateEngineService) {
        this.rateEngineService = rateEngineService;
    }

    public QuoteResultDto quote(QuoteRequest req) {
        QuoteResult result = rateEngineService.calculatePrice(req.getCheckIn(), req.getCheckOut(),
                req.getRoomTypeId(), req.getAdults(), req.getChildren(),
                req.getRatePlanId(), req.getPromotionCode());
        List<QuoteNightlyRateDto> nightly = new ArrayList<>();
        for (QuoteResult.NightlyBreakdown nb : result.getNightly()) {
            nightly.add(new QuoteNightlyRateDto(nb.getDate(), nb.getBaseRate(),
                    nb.getExtraPersonCharge(), nb.getDiscountAmount(), nb.getTaxesAmount(),
                    nb.getFeesAmount(), nb.getTotal()));
        }
        return new QuoteResultDto(nightly, result.getSubtotal(), result.getTotalDiscount(),
                result.getTotalTaxes(), result.getTotalFees(), result.getGrandTotal(),
                result.getRatePlanId());
    }
}
