package com.mipyme.arca.dto;

import java.util.List;


public class InvoiceResponse {

    private boolean success;
    private String resultado;
    private String cae;
    private String caeFchVto;
    private long cbteDesde;
    private long cbteHasta;
    private int puntoVenta;
    private int tipoComprobante;
    private String cbteFecha;
    private List<String> observaciones;
    private List<String> errores;
    private Long invoiceIndexId;


    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }

    public String getCaeFchVto() { return caeFchVto; }
    public void setCaeFchVto(String caeFchVto) { this.caeFchVto = caeFchVto; }

    public long getCbteDesde() { return cbteDesde; }
    public void setCbteDesde(long cbteDesde) { this.cbteDesde = cbteDesde; }

    public long getCbteHasta() { return cbteHasta; }
    public void setCbteHasta(long cbteHasta) { this.cbteHasta = cbteHasta; }

    public int getPuntoVenta() { return puntoVenta; }
    public void setPuntoVenta(int puntoVenta) { this.puntoVenta = puntoVenta; }

    public int getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(int tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getCbteFecha() { return cbteFecha; }
    public void setCbteFecha(String cbteFecha) { this.cbteFecha = cbteFecha; }

    public List<String> getObservaciones() { return observaciones; }
    public void setObservaciones(List<String> observaciones) { this.observaciones = observaciones; }

    public List<String> getErrores() { return errores; }
    public void setErrores(List<String> errores) { this.errores = errores; }

    public Long getInvoiceIndexId() { return invoiceIndexId; }
    public void setInvoiceIndexId(Long invoiceIndexId) { this.invoiceIndexId = invoiceIndexId; }
}
