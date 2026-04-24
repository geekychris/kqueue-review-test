package com.example.demo.controller;

import com.example.demo.model.Payment;
import com.example.demo.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("user_id").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String currency = (String) body.getOrDefault("currency", "USD");
        String cardNumber = (String) body.get("card_number");

        Payment payment = paymentService.processPayment(userId, amount, currency, cardNumber);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
        Payment payment = paymentService.getPayment(id);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/user/{userId}")
    public List<Payment> getUserPayments(@PathVariable Long userId) {
        return paymentService.getPaymentsByUser(userId);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<String> refundPayment(@PathVariable Long id) {
        String result = paymentService.refundPayment(id);
        return ResponseEntity.ok(result);
    }
}
