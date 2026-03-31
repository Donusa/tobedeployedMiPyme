package com.mipyme.arca.controller;

import com.mipyme.arca.dto.CsrResponse;
import com.mipyme.arca.service.ArcaService;
import org.springframework.http.ResponseEntity;
import com.mipyme.arca.model.ArcaConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.mipyme.arca.service.WsfeService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.web.multipart.MultipartFile;
import com.mipyme.arca.service.WsaaService;
import com.mipyme.company.PlanFeature;
import com.mipyme.company.PlanLimitService;
import com.mipyme.tenant.TenantContext;

@RestController
@RequestMapping("/api/arca")
public class ArcaController {

    private final ArcaService arcaService;
    private final WsfeService wsfeService;
    private final WsaaService wsaaService;
    private final PlanLimitService planLimitService;

    public ArcaController(ArcaService arcaService, WsfeService wsfeService, WsaaService wsaaService,
                          PlanLimitService planLimitService) {
        this.arcaService = arcaService;
        this.wsfeService = wsfeService;
        this.wsaaService = wsaaService;
        this.planLimitService = planLimitService;
    }

    @PostMapping("/csr")
    public ResponseEntity<CsrResponse> generateCsr(@RequestParam String companyName, @RequestParam String cuit) {
        planLimitService.assertPlanAccess(TenantContext.getCurrentTenant(), PlanFeature.FACTURACION_ARCA);
        try {
            CsrResponse response = arcaService.generateCsrAndPrivateKey(companyName, cuit);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/certificate")
    public ResponseEntity<Void> saveCertificate(@org.springframework.web.bind.annotation.RequestBody String certificate) {

        return ResponseEntity.ok().build();
    }

    @GetMapping("/config")
    public ResponseEntity<ArcaConfig> getArcaConfig() {
        planLimitService.assertPlanAccess(TenantContext.getCurrentTenant(), PlanFeature.FACTURACION_ARCA);
        try {
            ArcaConfig config = arcaService.getArcaConfig();
            if (config == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/config")
    public ResponseEntity<ArcaConfig> updateConfig(@org.springframework.web.bind.annotation.RequestBody ArcaConfig config) {
        planLimitService.assertPlanAccess(TenantContext.getCurrentTenant(), PlanFeature.FACTURACION_ARCA);
        try {
            ArcaConfig updatedConfig = arcaService.updateConfig(config);
            return ResponseEntity.ok(updatedConfig);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/config")
    public ResponseEntity<Void> deleteConfig() {
        planLimitService.assertPlanAccess(TenantContext.getCurrentTenant(), PlanFeature.FACTURACION_ARCA);
        try {
            arcaService.deleteConfig();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/state")
    public ResponseEntity<Void> saveWizardState(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> state) {
        planLimitService.assertPlanAccess(TenantContext.getCurrentTenant(), PlanFeature.FACTURACION_ARCA);
        try {
            Integer currentStep = (Integer) state.get("currentStep");
            Boolean wizardCompleted = (Boolean) state.get("wizardCompleted");
            arcaService.saveWizardState(currentStep, wizardCompleted);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/p12")
    public ResponseEntity<Resource> generateP12(
            @org.springframework.web.bind.annotation.RequestBody(required = false) java.util.Map<String, String> body,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String certificateContent
    ) {
        planLimitService.assertPlanAccess(TenantContext.getCurrentTenant(), PlanFeature.FACTURACION_ARCA);
        try {

            String finalPassword = null;
            String finalCertContent = null;

            if (body != null) {
                finalPassword = body.get("password");
                finalCertContent = body.get("certificateContent");
            }

            if (finalPassword == null && password != null) {
                finalPassword = password;
            }
            if (finalCertContent == null && certificateContent != null) {
                finalCertContent = certificateContent;
            }


            System.out.println("Generate P12 Request - Body present: " + (body != null));
            System.out.println("Generate P12 Request - Params present: password=" + (password != null) + ", cert=" + (certificateContent != null));

            if (finalPassword == null || finalCertContent == null) {
                return ResponseEntity.badRequest().build();
            }


            byte[] p12Bytes = arcaService.generateP12(finalCertContent, finalPassword);
            ByteArrayResource resource = new ByteArrayResource(p12Bytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alias.p12")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(p12Bytes.length)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/activate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> activate(@RequestParam("file") MultipartFile file, @RequestParam("password") String password) {
        planLimitService.assertPlanAccess(TenantContext.getCurrentTenant(), PlanFeature.FACTURACION_ARCA);
        try {
             wsaaService.authenticateWithP12(file, password, "wsfe");
             return ResponseEntity.ok("Activación Exitosa: Token obtenido y almacenado.");
        } catch (Exception e) {
             e.printStackTrace();
             return ResponseEntity.internalServerError().body("Error activando servicio: " + e.getMessage());
        }
    }

    @GetMapping("/check-connection")
    public ResponseEntity<String> checkConnection() {
        planLimitService.assertPlanAccess(TenantContext.getCurrentTenant(), PlanFeature.FACTURACION_ARCA);
        try {


            String response = wsfeService.getLastVoucher(1, 1);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error conectando con ARCA: " + e.getMessage());
        }
    }
}
