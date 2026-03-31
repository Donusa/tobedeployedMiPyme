package com.mipyme.company;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import com.mipyme.tenant.TenantContext;

@RestController
@RequestMapping("/api/schema")
public class SchemaDiagnosticController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/diagnostic")
    public ResponseEntity<List<String>> checkTables() {
        String currentSchema = TenantContext.getCurrentTenant();
        if (currentSchema == null) {
            return ResponseEntity.badRequest().body(List.of("No tenant context found"));
        }

        try {
            String sql = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
            List<String> tables = jdbcTemplate.queryForList(sql, String.class, currentSchema);
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of("Error: " + e.getMessage()));
        }
    }
}
