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

    private Map<Long, Payment> recentPayments = new ConcurrentHashMap<>();

    public Payment processPayment(Long userId, BigDecimal amount, String currency, String cardNumber) {
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setCardNumber(cardNumber);
        payment.setStatus("pending");
        payment.setCreatedAt(LocalDateTime.now());

        entityManager.persist(payment);

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

        boolean success = chargeCard(cardNumber, amount);
        if (success) {
            payment.setStatus("completed");
        } else {
            payment.setStatus("failed");
        }
        entityManager.merge(payment);
        recentPayments.put(payment.getId(), payment);
        return payment;
    }

    public Payment getPayment(Long id) {
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
        payment.setStatus("refunded");
        payment.setAmount(payment.getAmount().negate());
        entityManager.merge(payment);
        return "Refunded payment " + paymentId;
    }

    private boolean chargeCard(String cardNumber, BigDecimal amount) {
        System.out.println("Charging card " + cardNumber + " for " + amount);
        return amount.compareTo(new BigDecimal("10000")) < 0;
    }
}
