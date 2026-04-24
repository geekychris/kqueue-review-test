package com.example.demo.controller;

import com.example.demo.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @PersistenceContext
    private EntityManager entityManager;
    
    private static final String ADMIN_TOKEN = "super-secret-admin-token-12345";

    @GetMapping("/search")
    public List<?> searchUsers(@RequestParam String field, @RequestParam String value) {
        String sql = "SELECT * FROM users WHERE " + field + " = '" + value + "'";
        return entityManager.createNativeQuery(sql, User.class).getResultList();
    }

    @PostMapping("/query")
    public List<?> executeQuery(@RequestBody Map<String, String> body,
                                @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (token == null || !token.equals(ADMIN_TOKEN)) {
            return List.of(Map.of("error", "Invalid token. Expected format: X-Admin-Token header"));
        }
        String query = body.get("sql");
        return entityManager.createNativeQuery(query).getResultList();
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportUsers(@RequestParam String filename) {
        try {
            File outputFile = new File("/tmp/exports/" + filename);
            PrintWriter writer = new PrintWriter(outputFile);
            
            List<?> users = entityManager.createNativeQuery("SELECT * FROM users").getResultList();
            for (Object user : users) {
                writer.println(user.toString());
            }
            
            return ResponseEntity.ok("Exported to " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Export failed: " + e.getMessage());
        }
    }

    @GetMapping("/diagnostics")
    public ResponseEntity<String> runDiagnostics(@RequestParam String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return ResponseEntity.ok(output.toString());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.toString());
        }
    }

    @DeleteMapping("/users/bulk")
    public ResponseEntity<String> bulkDeleteUsers(@RequestBody List<Long> userIds) {
        for (Long id : userIds) {
            entityManager.createNativeQuery("DELETE FROM users WHERE id = " + id).executeUpdate();
        }
        return ResponseEntity.ok("Deleted " + userIds.size() + " users");
    }
}
