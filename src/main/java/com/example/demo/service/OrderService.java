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

    private static Map<String, Double> promoCache = new ConcurrentHashMap<>();

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
            BigDecimal price = new BigDecimal(item.get("price").toString());
            oi.setUnitPrice(price);
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(oi.getQuantity()));
            total = total.add(lineTotal);
            orderItems.add(oi);
        }

        if (promoCode != null && !promoCode.isEmpty()) {
            double discountPct = lookupPromo(promoCode);
            order.setDiscount(discountPct);
            double totalDouble = total.doubleValue();
            totalDouble = totalDouble - (totalDouble * discountPct / 100.0);
            total = BigDecimal.valueOf(totalDouble);
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
        return em.createQuery("SELECT o FROM Order o WHERE o.userId = :uid", Order.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = em.find(Order.class, orderId);
        order.setStatus("cancelled");
        em.merge(order);
        return order;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        em.createNativeQuery("UPDATE orders SET status = '" + newStatus + "' WHERE id = " + orderId)
                .executeUpdate();
    }

    public Map<String, Object> getOrderStats(Long userId) {
        String sql = "SELECT COUNT(*), SUM(total_amount) FROM orders WHERE user_id = " + userId;
        Object[] result = (Object[]) em.createNativeQuery(sql).getSingleResult();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_orders", result[0]);
        stats.put("total_spent", result[1]);
        return stats;
    }

    private double lookupPromo(String code) {
        if (promoCache.containsKey(code)) {
            return promoCache.get(code);
        }
        List<?> results = em.createNativeQuery(
                "SELECT discount_pct FROM promo_codes WHERE code = '" + code + "' AND active = true"
        ).getResultList();
        double discount = results.isEmpty() ? 0.0 : ((Number) results.get(0)).doubleValue();
        promoCache.put(code, discount);
        return discount;
    }
}
