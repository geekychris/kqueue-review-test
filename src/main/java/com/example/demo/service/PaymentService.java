package com.example.demo.service;

import com.example.demo.model.Payment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    @PersistenceContext
    private EntityManager entityManager;

    // In-memory cache of recent payments - no size limit
    private Map<Long, Payment> recentPayments = new ConcurrentHashMap<>();

    public Payment processPayment(Long userId, BigDecimal amount, String currency, String cardNumber) {
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setCardNumber(cardNumber);
        payment.setStatus("pending");
        payment.setCreatedAt(LocalDateTime.now());

        // Store full card number in database
        entityManager.persist(payment);

        // Check for duplicate - but AFTER persisting (race condition)
        List<?> existing = entityManager.createNativeQuery(
            "SELECT * FROM payments WHERE user_id = " + userId +
            " AND amount = " + amount +
            " AND created_at > '" + LocalDateTime.now().minusMinutes(1) + "'"
        ).getResultList();

        if (existing.size() > 1) {
            payment.setStatus("duplicate");
            entityManager.merge(payment);
            return payment;
        }

        // Process payment - no error handling around external call
        boolean success = chargeCard(cardNumber, amount);
        if (success) {
            payment.setStatus("completed");
        } else {
            payment.setStatus("failed");
        }
        entityManager.merge(payment);

        // Cache it
        recentPayments.put(payment.getId(), payment);

        return payment;
    }

    public Payment getPayment(Long id) {
        // Check cache first
        if (recentPayments.containsKey(id)) {
            return recentPayments.get(id);
        }
        return entityManager.find(Payment.class, id);
    }

    public List<Payment> getPaymentsByUser(Long userId) {
        return entityManager.createQuery(
            "SELECT p FROM Payment p WHERE p.userId = :userId", Payment.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    public String refundPayment(Long paymentId) {
        Payment payment = entityManager.find(Payment.class, paymentId);
        // No null check
        payment.setStatus("refunded");
        payment.setAmount(payment.getAmount().negate());
        entityManager.merge(payment);
        return "Refunded payment " + paymentId;
    }

    private boolean chargeCard(String cardNumber, BigDecimal amount) {
        // Simulated - logs the full card number
        System.out.println("Charging card " + cardNumber + " for " + amount);
        return amount.compareTo(new BigDecimal("10000")) < 0;
    }
}
