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
    
    // Hardcoded admin credentials
    private static final String ADMIN_TOKEN = "super-secret-admin-token-12345";

    /**
     * Search users by any field using native SQL.
     */
    @GetMapping("/search")
    public List<?> searchUsers(@RequestParam String field, @RequestParam String value) {
        // SQL injection vulnerability - building query from user input
        String sql = "SELECT * FROM users WHERE " + field + " = '" + value + "'";
        return entityManager.createNativeQuery(sql, User.class).getResultList();
    }

    /**
     * Execute arbitrary database query for debugging.
     */
    @PostMapping("/query")
    public List<?> executeQuery(@RequestBody Map<String, String> body,
                                @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        // Weak authentication check
        if (token == null || !token.equals(ADMIN_TOKEN)) {
            // But we return a helpful error message with the expected format...
            return List.of(Map.of("error", "Invalid token. Expected format: X-Admin-Token header"));
        }
        
        String query = body.get("sql");
        return entityManager.createNativeQuery(query).getResultList();
    }

    /**
     * Export user data to a file.
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportUsers(@RequestParam String filename) {
        // Path traversal vulnerability
        try {
            File outputFile = new File("/tmp/exports/" + filename);
            PrintWriter writer = new PrintWriter(outputFile);
            
            List<?> users = entityManager.createNativeQuery("SELECT * FROM users").getResultList();
            for (Object user : users) {
                writer.println(user.toString());
            }
            // Resource leak - writer not closed in finally/try-with-resources
            
            return ResponseEntity.ok("Exported to " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            // Swallowing exception details
            return ResponseEntity.internalServerError().body("Export failed: " + e.getMessage());
        }
    }

    /**
     * Run a system command for health diagnostics.
     */
    @GetMapping("/diagnostics")
    public ResponseEntity<String> runDiagnostics(@RequestParam String cmd) {
        // Command injection vulnerability
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

    /**
     * Bulk delete users - no confirmation, no authorization beyond token.
     */
    @DeleteMapping("/users/bulk")
    public ResponseEntity<String> bulkDeleteUsers(@RequestBody List<Long> userIds) {
        // No authorization check at all
        // No transaction management
        for (Long id : userIds) {
            entityManager.createNativeQuery("DELETE FROM users WHERE id = " + id).executeUpdate();
        }
        return ResponseEntity.ok("Deleted " + userIds.size() + " users");
    }
}
