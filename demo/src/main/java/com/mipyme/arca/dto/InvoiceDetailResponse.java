package com.mipyme.arca.dto;

import java.util.List;


public class InvoiceDetailResponse {

    private boolean success;
    private int ptoVta;
    private int cbteTipo;
    private long cbteNro;
    private String cbteFch;
    private int concepto;
    private int docTipo;
    private String docNro;
    private double impTotal;
    private double impTotConc;
    private double impNeto;
    private double impOpEx;
    private double impTrib;
    private double impIVA;
    private String monId;
    private double monCotiz;
    private String cae;
    private String caeFchVto;
    private String resultado;
    private String fchServDesde;
    private String fchServHasta;
    private String fchVtoPago;
    private String fchProceso;

    private List<IvaDetail> ivaDetails;
    private List<TributoDetail> tributoDetails;
    private List<String> observaciones;
    private List<String> errores;



    public static class IvaDetail {
        private int id;
        private double baseImp;
        private double importe;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public double getBaseImp() { return baseImp; }
        public void setBaseImp(double baseImp) { this.baseImp = baseImp; }
        public double getImporte() { return importe; }
        public void setImporte(double importe) { this.importe = importe; }
    }

    public static class TributoDetail {
        private int id;
        private String desc;
        private double baseImp;
        private double alic;
        private double importe;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getDesc() { return desc; }
        public void setDesc(String desc) { this.desc = desc; }
        public double getBaseImp() { return baseImp; }
        public void setBaseImp(double baseImp) { this.baseImp = baseImp; }
        public double getAlic() { return alic; }
        public void setAlic(double alic) { this.alic = alic; }
        public double getImporte() { return importe; }
        public void setImporte(double importe) { this.importe = importe; }
    }



    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public int getPtoVta() { return ptoVta; }
    public void setPtoVta(int ptoVta) { this.ptoVta = ptoVta; }

    public int getCbteTipo() { return cbteTipo; }
    public void setCbteTipo(int cbteTipo) { this.cbteTipo = cbteTipo; }

    public long getCbteNro() { return cbteNro; }
    public void setCbteNro(long cbteNro) { this.cbteNro = cbteNro; }

    public String getCbteFch() { return cbteFch; }
    public void setCbteFch(String cbteFch) { this.cbteFch = cbteFch; }

    public int getConcepto() { return concepto; }
    public void setConcepto(int concepto) { this.concepto = concepto; }

    public int getDocTipo() { return docTipo; }
    public void setDocTipo(int docTipo) { this.docTipo = docTipo; }

    public String getDocNro() { return docNro; }
    public void setDocNro(String docNro) { this.docNro = docNro; }

    public double getImpTotal() { return impTotal; }
    public void setImpTotal(double impTotal) { this.impTotal = impTotal; }

    public double getImpTotConc() { return impTotConc; }
    public void setImpTotConc(double impTotConc) { this.impTotConc = impTotConc; }

    public double getImpNeto() { return impNeto; }
    public void setImpNeto(double impNeto) { this.impNeto = impNeto; }

    public double getImpOpEx() { return impOpEx; }
    public void setImpOpEx(double impOpEx) { this.impOpEx = impOpEx; }

    public double getImpTrib() { return impTrib; }
    public void setImpTrib(double impTrib) { this.impTrib = impTrib; }

    public double getImpIVA() { return impIVA; }
    public void setImpIVA(double impIVA) { this.impIVA = impIVA; }

    public String getMonId() { return monId; }
    public void setMonId(String monId) { this.monId = monId; }

    public double getMonCotiz() { return monCotiz; }
    public void setMonCotiz(double monCotiz) { this.monCotiz = monCotiz; }

    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }

    public String getCaeFchVto() { return caeFchVto; }
    public void setCaeFchVto(String caeFchVto) { this.caeFchVto = caeFchVto; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getFchServDesde() { return fchServDesde; }
    public void setFchServDesde(String fchServDesde) { this.fchServDesde = fchServDesde; }

    public String getFchServHasta() { return fchServHasta; }
    public void setFchServHasta(String fchServHasta) { this.fchServHasta = fchServHasta; }

    public String getFchVtoPago() { return fchVtoPago; }
    public void setFchVtoPago(String fchVtoPago) { this.fchVtoPago = fchVtoPago; }

    public String getFchProceso() { return fchProceso; }
    public void setFchProceso(String fchProceso) { this.fchProceso = fchProceso; }

    public List<IvaDetail> getIvaDetails() { return ivaDetails; }
    public void setIvaDetails(List<IvaDetail> ivaDetails) { this.ivaDetails = ivaDetails; }

    public List<TributoDetail> getTributoDetails() { return tributoDetails; }
    public void setTributoDetails(List<TributoDetail> tributoDetails) { this.tributoDetails = tributoDetails; }

    public List<String> getObservaciones() { return observaciones; }
    public void setObservaciones(List<String> observaciones) { this.observaciones = observaciones; }

    public List<String> getErrores() { return errores; }
    public void setErrores(List<String> errores) { this.errores = errores; }
}
