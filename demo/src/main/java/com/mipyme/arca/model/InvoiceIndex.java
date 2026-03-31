package com.mipyme.arca.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_index", uniqueConstraints = {
        @UniqueConstraint(name = "uk_fiscal_key", columnNames = {"cuit_emisor", "pto_vta", "cbte_tipo", "cbte_nro"}),
        @UniqueConstraint(name = "uk_idempotency", columnNames = {"idempotency_key"})
})
public class InvoiceIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String idempotencyKey;

    @Column(name = "cuit_emisor", nullable = false, length = 11, columnDefinition = "CHAR(11)")
    private String cuitEmisor;

    @Column(name = "pto_vta", nullable = false)
    private Short ptoVta;

    @Column(name = "cbte_tipo", nullable = false)
    private Short cbteTipo;

    @Column(name = "cbte_nro", nullable = false)
    private Long cbteNro;

    @Column(name = "cbte_fch", nullable = false)
    private LocalDate cbteFch;

    @Column(name = "concepto", nullable = false)
    private Byte concepto;

    @Column(name = "doc_tipo", nullable = false)
    private Byte docTipo;

    @Column(name = "doc_nro_token", length = 64, columnDefinition = "CHAR(64)")
    private String docNroToken;

    @Column(name = "doc_last4", length = 4, columnDefinition = "CHAR(4)")
    private String docLast4;

    @Column(name = "imp_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal impTotal;

    @Column(name = "imp_neto", nullable = false, precision = 15, scale = 2)
    private BigDecimal impNeto;

    @Column(name = "imp_iva", nullable = false, precision = 15, scale = 2)
    private BigDecimal impIva;

    @Column(name = "imp_trib", nullable = false, precision = 15, scale = 2)
    private BigDecimal impTrib = BigDecimal.ZERO;

    @Column(name = "imp_tot_conc", nullable = false, precision = 15, scale = 2)
    private BigDecimal impTotConc = BigDecimal.ZERO;

    @Column(name = "imp_op_ex", nullable = false, precision = 15, scale = 2)
    private BigDecimal impOpEx = BigDecimal.ZERO;

    @Column(name = "mon_id", nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String monId;

    @Column(name = "mon_cotiz", nullable = false, precision = 10, scale = 6)
    private BigDecimal monCotiz;

    @Column(name = "cae", nullable = false, length = 14, columnDefinition = "CHAR(14)")
    private String cae;

    @Column(name = "cae_fch_vto", nullable = false)
    private LocalDate caeFchVto;

    @Column(name = "resultado", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String resultado;

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "request_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String requestHash;

    @Column(name = "response_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String responseHash;

    @Column(name = "obs_codes", length = 200)
    private String obsCodes;

    @Column(name = "obs_msg", length = 500)
    private String obsMsg;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }



    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getCuitEmisor() { return cuitEmisor; }
    public void setCuitEmisor(String cuitEmisor) { this.cuitEmisor = cuitEmisor; }

    public Short getPtoVta() { return ptoVta; }
    public void setPtoVta(Short ptoVta) { this.ptoVta = ptoVta; }

    public Short getCbteTipo() { return cbteTipo; }
    public void setCbteTipo(Short cbteTipo) { this.cbteTipo = cbteTipo; }

    public Long getCbteNro() { return cbteNro; }
    public void setCbteNro(Long cbteNro) { this.cbteNro = cbteNro; }

    public LocalDate getCbteFch() { return cbteFch; }
    public void setCbteFch(LocalDate cbteFch) { this.cbteFch = cbteFch; }

    public Byte getConcepto() { return concepto; }
    public void setConcepto(Byte concepto) { this.concepto = concepto; }

    public Byte getDocTipo() { return docTipo; }
    public void setDocTipo(Byte docTipo) { this.docTipo = docTipo; }

    public String getDocNroToken() { return docNroToken; }
    public void setDocNroToken(String docNroToken) { this.docNroToken = docNroToken; }

    public String getDocLast4() { return docLast4; }
    public void setDocLast4(String docLast4) { this.docLast4 = docLast4; }

    public BigDecimal getImpTotal() { return impTotal; }
    public void setImpTotal(BigDecimal impTotal) { this.impTotal = impTotal; }

    public BigDecimal getImpNeto() { return impNeto; }
    public void setImpNeto(BigDecimal impNeto) { this.impNeto = impNeto; }

    public BigDecimal getImpIva() { return impIva; }
    public void setImpIva(BigDecimal impIva) { this.impIva = impIva; }

    public BigDecimal getImpTrib() { return impTrib; }
    public void setImpTrib(BigDecimal impTrib) { this.impTrib = impTrib; }

    public BigDecimal getImpTotConc() { return impTotConc; }
    public void setImpTotConc(BigDecimal impTotConc) { this.impTotConc = impTotConc; }

    public BigDecimal getImpOpEx() { return impOpEx; }
    public void setImpOpEx(BigDecimal impOpEx) { this.impOpEx = impOpEx; }

    public String getMonId() { return monId; }
    public void setMonId(String monId) { this.monId = monId; }

    public BigDecimal getMonCotiz() { return monCotiz; }
    public void setMonCotiz(BigDecimal monCotiz) { this.monCotiz = monCotiz; }

    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }

    public LocalDate getCaeFchVto() { return caeFchVto; }
    public void setCaeFchVto(LocalDate caeFchVto) { this.caeFchVto = caeFchVto; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }

    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }

    public String getResponseHash() { return responseHash; }
    public void setResponseHash(String responseHash) { this.responseHash = responseHash; }

    public String getObsCodes() { return obsCodes; }
    public void setObsCodes(String obsCodes) { this.obsCodes = obsCodes; }

    public String getObsMsg() { return obsMsg; }
    public void setObsMsg(String obsMsg) { this.obsMsg = obsMsg; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }


    public String toFiscalKey() {
        return cuitEmisor + ":" + ptoVta + ":" + cbteTipo + ":" + cbteNro;
    }
}
