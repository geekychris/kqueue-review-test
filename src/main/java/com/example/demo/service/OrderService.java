package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    @PersistenceContext
    private EntityManager em;

    // Unbounded cache - will grow forever, memory leak
    private static Map<String, Double> promoCache = new ConcurrentHashMap<>();

    // Credentials loaded from env but stored in a static field
    private static String paymentApiKey = System.getenv("PAYMENT_API_KEY");
    private static String shippingApiKey = System.getenv("SHIPPING_API_KEY");

    @Transactional
    public Order createOrder(Long userId, List<Map<String, Object>> items, String shippingAddr, String promoCode) {
        Order order = new Order();
        order.setUserId(userId);
        order.setShippingAddress(shippingAddr);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("pending");

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> item : items) {
            OrderItem oi = new OrderItem();
            oi.setProductId(Long.valueOf(item.get("product_id").toString()));
            oi.setProductName((String) item.get("name"));
            oi.setQuantity((int) item.get("quantity"));

            // BUG: trusting client-provided price instead of looking up from product catalog
            BigDecimal price = new BigDecimal(item.get("price").toString());
            oi.setUnitPrice(price);

            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(oi.getQuantity()));
            total = total.add(lineTotal);
            orderItems.add(oi);
        }

        // BUG: using double for money calculations - precision loss
        if (promoCode != null && !promoCode.isEmpty()) {
            double discountPct = lookupPromo(promoCode);
            order.setDiscount(discountPct);
            double totalDouble = total.doubleValue();
            totalDouble = totalDouble - (totalDouble * discountPct / 100.0);
            total = BigDecimal.valueOf(totalDouble);
            order.setPromoCode(promoCode);
        }

        order.setTotalAmount(total);
        order.setItems(orderItems);
        em.persist(order);
        return order;
    }

    public Order getOrder(Long orderId) {
        return em.find(Order.class, orderId);
    }

    public List<Order> getOrdersByUser(Long userId) {
        // N+1 query problem - EAGER fetch loads all items for every order
        return em.createQuery("SELECT o FROM Order o WHERE o.userId = :uid", Order.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = em.find(Order.class, orderId);
        // BUG: no null check - NPE if order not found
        // BUG: no ownership check - anyone can cancel anyone's order
        // BUG: no status check - can cancel already-shipped orders
        order.setStatus("cancelled");
        order.setUpdatedAt(LocalDateTime.now());
        em.merge(order);
        return order;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        // SECURITY: SQL injection via string concatenation
        em.createNativeQuery("UPDATE orders SET status = '" + newStatus + "' WHERE id = " + orderId)
                .executeUpdate();
    }

    public Map<String, Object> getOrderStats(Long userId) {
        // SECURITY: SQL injection
        String sql = "SELECT COUNT(*), SUM(total_amount) FROM orders WHERE user_id = " + userId;
        Object[] result = (Object[]) em.createNativeQuery(sql).getSingleResult();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_orders", result[0]);
        stats.put("total_spent", result[1]);
        // BUG: leaking API key info in stats response
        stats.put("payment_connected", paymentApiKey != null && !paymentApiKey.isEmpty());
        stats.put("payment_key_prefix", paymentApiKey != null ? paymentApiKey.substring(0, Math.min(8, paymentApiKey.length())) : "not set");
        return stats;
    }

    private double lookupPromo(String code) {
        if (promoCache.containsKey(code)) {
            return promoCache.get(code);
        }
        // SECURITY: SQL injection in promo code lookup
        List<?> results = em.createNativeQuery(
                "SELECT discount_pct FROM promo_codes WHERE code = '" + code + "' AND active = true"
        ).getResultList();

        double discount = results.isEmpty() ? 0.0 : ((Number) results.get(0)).doubleValue();
        promoCache.put(code, discount);
        return discount;
    }
}
