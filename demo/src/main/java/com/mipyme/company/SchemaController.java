package com.mipyme.company;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mipyme.tenant.TenantContext;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final CompanySchemaService companySchemaService;

    public SchemaController(CompanySchemaService companySchemaService) {
        this.companySchemaService = companySchemaService;
    }

    @PostMapping("/update")
    public ResponseEntity<Void> updateSchema() {
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant == null) {
            return ResponseEntity.badRequest().build();
        }
        companySchemaService.ensureTenantSchemaAndTables(currentTenant);
        return ResponseEntity.ok().build();
    }
}
