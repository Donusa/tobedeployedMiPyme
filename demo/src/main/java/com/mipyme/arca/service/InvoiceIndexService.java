package com.mipyme.arca.service;

import com.mipyme.arca.dto.InvoiceRequest;
import com.mipyme.arca.dto.InvoiceResponse;
import com.mipyme.arca.model.InvoiceIndex;
import com.mipyme.arca.repository.InvoiceIndexRepository;
import com.mipyme.sales.service.SaleService;
import com.mipyme.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class InvoiceIndexService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceIndexService.class);

    private final InvoiceIndexRepository invoiceIndexRepository;
    private final InvoiceAuditService auditService;
    private final SaleService saleService;
    private final byte[] hmacSecret;

    public InvoiceIndexService(InvoiceIndexRepository invoiceIndexRepository,
                               InvoiceAuditService auditService,
                               SaleService saleService) {
        this.invoiceIndexRepository = invoiceIndexRepository;
        this.auditService = auditService;
        this.saleService = saleService;

        String envSecret = System.getenv("INVOICE_HMAC_SECRET");
        if (envSecret != null && !envSecret.isEmpty()) {
            this.hmacSecret = envSecret.getBytes(StandardCharsets.UTF_8);
        } else {
            logger.warn("INVOICE_HMAC_SECRET not set. Using ARCA_MASTER_KEY fallback.");
            String fallback = System.getenv("ARCA_MASTER_KEY");
            this.hmacSecret = (fallback != null && !fallback.isEmpty())
                    ? fallback.getBytes(StandardCharsets.UTF_8)
                    : "dev-only-hmac-secret-do-not-use-in-prod".getBytes(StandardCharsets.UTF_8);
        }
    }




    @Transactional(readOnly = true)
    public Optional<InvoiceIndex> findByIdempotencyKey(InvoiceRequest req, String cuitEmisor) {
        String key = computeIdempotencyKey(req, cuitEmisor);
        return invoiceIndexRepository.findByIdempotencyKey(key);
    }


    public String computeIdempotencyKey(InvoiceRequest req, String cuitEmisor) {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null) tenant = "default";

        StringBuilder sb = new StringBuilder();
        sb.append(tenant).append('|');
        sb.append(cuitEmisor).append('|');
        sb.append(req.getPuntoVenta()).append('|');
        sb.append(req.getTipoComprobante()).append('|');
        sb.append(req.getConcepto()).append('|');
        sb.append(req.getFecha()).append('|');
        sb.append(req.getDocTipo()).append('|');
        sb.append(safe(req.getDocNro())).append('|');
        sb.append(req.getMonId()).append('|');
        sb.append(fmt2(req.getMonCotiz())).append('|');
        sb.append(fmt2(req.getImpTotal())).append('|');
        sb.append(fmt2(req.getImpNeto())).append('|');
        sb.append(fmt2(req.getImpIVA())).append('|');
        sb.append(fmt2(req.getImpTrib())).append('|');
        sb.append(fmt2(req.getImpTotConc())).append('|');
        sb.append(fmt2(req.getImpOpEx())).append('|');
        sb.append(safe(req.getFchServDesde())).append('|');
        sb.append(safe(req.getFchServHasta())).append('|');
        sb.append(safe(req.getFchVtoPago())).append('|');


        if (req.getItems() != null) {
            req.getItems().stream()
                    .sorted(Comparator.comparingInt(InvoiceRequest.InvoiceItem::getIvaId)
                            .thenComparingDouble(InvoiceRequest.InvoiceItem::getBaseImp))
                    .forEach(item -> sb.append(item.getIvaId())
                            .append(':').append(fmt2(item.getBaseImp()))
                            .append(':').append(fmt2(item.getIvaImporte()))
                            .append(';'));
        }
        sb.append('|');


        if (req.getTributos() != null) {
            req.getTributos().stream()
                    .sorted(Comparator.comparingInt(InvoiceRequest.InvoiceTributo::getId))
                    .forEach(t -> sb.append(t.getId())
                            .append(':').append(fmt2(t.getBaseImp()))
                            .append(':').append(fmt2(t.getAlic()))
                            .append(':').append(fmt2(t.getImporte()))
                            .append(';'));
        }

        return sha256(sb.toString());
    }




    @Transactional
    public InvoiceIndex persistEmission(InvoiceRequest req, InvoiceResponse resp,
                                         String cuitEmisor, String requestHash, String responseHash) {
        String actor = getActor();
        String idempotencyKey = computeIdempotencyKey(req, cuitEmisor);

        InvoiceIndex index = new InvoiceIndex();
        index.setIdempotencyKey(idempotencyKey);
        index.setCuitEmisor(cuitEmisor);
        index.setPtoVta((short) resp.getPuntoVenta());
        index.setCbteTipo((short) resp.getTipoComprobante());
        index.setCbteNro(resp.getCbteDesde());
        index.setCbteFch(parseDate(resp.getCbteFecha()));
        index.setConcepto((byte) req.getConcepto());
        index.setDocTipo((byte) req.getDocTipo());


        String docNro = req.getDocNro();
        if (docNro != null && !docNro.isEmpty() && !"0".equals(docNro)) {
            index.setDocNroToken(hmacSha256(req.getDocTipo() + "|" + docNro));
            if (docNro.length() >= 4) {
                index.setDocLast4(docNro.substring(docNro.length() - 4));
            }
        }

        index.setImpTotal(BigDecimal.valueOf(req.getImpTotal()));
        index.setImpNeto(BigDecimal.valueOf(req.getImpNeto()));
        index.setImpIva(BigDecimal.valueOf(req.getImpIVA()));
        index.setImpTrib(BigDecimal.valueOf(req.getImpTrib()));
        index.setImpTotConc(BigDecimal.valueOf(req.getImpTotConc()));
        index.setImpOpEx(BigDecimal.valueOf(req.getImpOpEx()));
        index.setMonId(req.getMonId());
        index.setMonCotiz(BigDecimal.valueOf(req.getMonCotiz()));
        index.setCae(resp.getCae() != null ? resp.getCae() : "");
        index.setCaeFchVto(resp.getCaeFchVto() != null && !resp.getCaeFchVto().isEmpty()
                ? parseDate(resp.getCaeFchVto()) : index.getCbteFch());
        index.setResultado(resp.getResultado() != null ? resp.getResultado() : "R");
        index.setSaleId(req.getSaleId());
        index.setRequestHash(requestHash);
        index.setResponseHash(responseHash);


        if (resp.getObservaciones() != null && !resp.getObservaciones().isEmpty()) {
            index.setObsCodes(extractCodes(resp.getObservaciones()));
            index.setObsMsg(truncate(String.join("; ", resp.getObservaciones()), 500));
        }
        if (resp.getErrores() != null && !resp.getErrores().isEmpty()) {
            String errMsg = truncate(String.join("; ", resp.getErrores()), 500);
            index.setObsMsg(index.getObsMsg() != null ? index.getObsMsg() + " | " + errMsg : errMsg);
        }

        index.setCreatedBy(actor);
        index.setCreatedAt(LocalDateTime.now());

        InvoiceIndex saved = invoiceIndexRepository.save(index);


        if ("A".equals(saved.getResultado()) && req.getSaleId() != null) {
            try {
                saleService.markAsFacturado(req.getSaleId());
            } catch (Exception e) {
                logger.warn("Could not mark sale {} as facturado: {}", req.getSaleId(), e.getMessage());
            }
        }


        String eventType = "A".equals(saved.getResultado()) ? "EMIT_SUCCESS" : "EMIT_REJECT";
        auditService.append(eventType, saved.getId(), saved.toFiscalKey(),
                actor, "resultado=" + saved.getResultado(), requestHash, responseHash);

        logger.info("Invoice index persisted: id={} fiscal_key={} resultado={}",
                saved.getId(), saved.toFiscalKey(), saved.getResultado());

        return saved;
    }


    @Transactional
    public void recordEmitError(InvoiceRequest req, String cuitEmisor, String errorDetail,
                                 String requestHash) {
        String actor = getActor();
        String fiscalKey = cuitEmisor + ":" + req.getPuntoVenta() + ":" + req.getTipoComprobante() + ":?";
        auditService.append("EMIT_ERROR", null, fiscalKey, actor,
                truncate(errorDetail, 500), requestHash, null);
    }



    @Transactional(readOnly = true)
    public Optional<InvoiceIndex> findById(Long id) {
        return invoiceIndexRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<InvoiceIndex> findByDateRange(LocalDate from, LocalDate to, Short cbteTipo, String resultado) {
        return invoiceIndexRepository.findByFilters(from, to, cbteTipo, resultado);
    }

    @Transactional(readOnly = true)
    public Optional<InvoiceIndex> findByFiscalKey(String cuitEmisor, Short ptoVta, Short cbteTipo, Long cbteNro) {
        return invoiceIndexRepository.findByCuitEmisorAndPtoVtaAndCbteTipoAndCbteNro(
                cuitEmisor, ptoVta, cbteTipo, cbteNro);
    }


    @Transactional
    public void recordView(Long invoiceIndexId) {
        String actor = getActor();
        auditService.append("VIEW", invoiceIndexId, null, actor, null, null, null);
    }


    @Transactional
    public void recordExport(String detail) {
        String actor = getActor();
        auditService.append("EXPORT", null, null, actor, truncate(detail, 500), null, null);
    }



    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    private String hmacSha256(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
            byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HmacSHA256 unavailable", e);
        }
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return LocalDate.now();

        String clean = dateStr.replace("-", "");
        if (clean.length() == 8) {
            return LocalDate.parse(clean, DateTimeFormatter.BASIC_ISO_DATE);
        }
        return LocalDate.parse(dateStr);
    }

    private static String getActor() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static String fmt2(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private static String extractCodes(List<String> observations) {

        StringBuilder codes = new StringBuilder();
        for (String obs : observations) {
            String code = obs.contains(":") ? obs.substring(0, obs.indexOf(':')).trim() : obs.trim();
            if (!code.isEmpty()) {
                if (codes.length() > 0) codes.append(',');
                codes.append(code);
            }
        }
        String result = codes.toString();
        return result.length() <= 200 ? result : result.substring(0, 200);
    }
}
