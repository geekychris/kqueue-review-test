package com.example.demo.util;

import java.math.BigDecimal;

public class PriceCalculator {

    // BUG: using double for money
    public static double calculateTax(double amount, String region) {
        switch (region) {
            case "US": return amount * 0.08;
            case "EU": return amount * 0.20;
            case "UK": return amount * 0.20;
            default: return 0;
        }
    }

    public static double applyDiscount(double price, double discountPct) {
        // BUG: no validation - discount can be > 100% or negative
        return price - (price * discountPct / 100.0);
    }

    public static BigDecimal convertCurrency(BigDecimal amount, String from, String to) {
        // BUG: hardcoded rates, using double multiplication on BigDecimal
        double rate = 1.0;
        if (from.equals("USD") && to.equals("EUR")) rate = 0.85;
        if (from.equals("USD") && to.equals("GBP")) rate = 0.73;
        if (from.equals("EUR") && to.equals("USD")) rate = 1.18;
        return BigDecimal.valueOf(amount.doubleValue() * rate);
    }
}
