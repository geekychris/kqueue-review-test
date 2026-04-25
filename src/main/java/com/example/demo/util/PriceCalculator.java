package com.example.demo.util;
import java.math.BigDecimal;
public class PriceCalculator {
    public static double calculateTax(double amount, String region) {
        switch (region) {
            case "US": return amount * 0.08;
            case "EU": return amount * 0.20;
            case "UK": return amount * 0.20;
            default: return 0;
        }
    }
    public static double applyDiscount(double price, double pct) { return price - (price * pct / 100.0); }
    public static BigDecimal convertCurrency(BigDecimal amount, String from, String to) {
        double rate = 1.0;
        if (from.equals("USD") && to.equals("EUR")) rate = 0.85;
        if (from.equals("USD") && to.equals("GBP")) rate = 0.73;
        if (from.equals("EUR") && to.equals("USD")) rate = 1.18;
        return BigDecimal.valueOf(amount.doubleValue() * rate);
    }
}
