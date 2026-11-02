package com.cinex.cinema.common;

import java.math.BigDecimal;

public final class SeatPricing {

    private static final BigDecimal VIP_EXTRA = new BigDecimal("30000");

    private SeatPricing() {
    }

    public static BigDecimal priceFor(BigDecimal basePrice, String seatType) {
        if (basePrice == null) {
            return BigDecimal.ZERO;
        }
        if (seatType == null) {
            return basePrice;
        }
        return switch (seatType.toUpperCase()) {
            case "VIP" -> basePrice.add(VIP_EXTRA);
            case "COUPLE" -> basePrice.multiply(BigDecimal.valueOf(2));
            default -> basePrice;
        };
    }
}
