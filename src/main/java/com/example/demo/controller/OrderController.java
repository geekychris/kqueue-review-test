package com.example.demo.controller;
import com.example.demo.model.Order; import com.example.demo.service.OrderService;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.Map;
@RestController @RequestMapping("/api/orders")
public class OrderController {
    private final OrderService svc;
    public OrderController(OrderService svc) { this.svc = svc; }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("user_id").toString());
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        String addr = (String) body.get("shipping_address");
        String promo = (String) body.get("promo_code");
        return ResponseEntity.ok(svc.createOrder(userId, items, addr, promo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable Long id) { return ResponseEntity.ok(svc.getOrder(id)); }

    @GetMapping("/user/{userId}")
    public List<Order> byUser(@PathVariable Long userId) { return svc.getOrdersByUser(userId); }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancel(@PathVariable Long id) { return ResponseEntity.ok(svc.cancelOrder(id)); }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        svc.updateOrderStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/{userId}")
    public Map<String, Object> stats(@PathVariable Long userId) { return svc.getOrderStats(userId); }
}
