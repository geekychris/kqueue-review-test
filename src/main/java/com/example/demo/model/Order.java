package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private String shippingAddress;
    private String promoCode;
    private double discount;
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal t) { this.totalAmount = t; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String a) { this.shippingAddress = a; }
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String c) { this.promoCode = c; }
    public double getDiscount() { return discount; }
    public void setDiscount(double d) { this.discount = d; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
