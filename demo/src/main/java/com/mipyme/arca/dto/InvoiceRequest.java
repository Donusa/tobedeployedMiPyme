package com.mipyme.arca.dto;

import java.util.List;


public class InvoiceRequest {

    private int puntoVenta;
    private int tipoComprobante;
    private int concepto;
    private int docTipo;
    private String docNro;
    private int condicionIvaReceptor;
    private String fecha;
    private String fchServDesde;
    private String fchServHasta;
    private String fchVtoPago;
    private String monId;
    private double monCotiz;


    private double impNeto;
    private double impIVA;
    private double impTrib;
    private double impTotal;
    private double impTotConc;
    private double impOpEx;

    private List<InvoiceItem> items;
    private List<InvoiceTributo> tributos;


    private Long saleId;


    public static class InvoiceItem {
        private String codigo;
        private String descripcion;
        private double cantidad;
        private double precioUnitario;
        private int ivaId;
        private double baseImp;
        private double ivaImporte;

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public double getCantidad() { return cantidad; }
        public void setCantidad(double cantidad) { this.cantidad = cantidad; }
        public double getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
        public int getIvaId() { return ivaId; }
        public void setIvaId(int ivaId) { this.ivaId = ivaId; }
        public double getBaseImp() { return baseImp; }
        public void setBaseImp(double baseImp) { this.baseImp = baseImp; }
        public double getIvaImporte() { return ivaImporte; }
        public void setIvaImporte(double ivaImporte) { this.ivaImporte = ivaImporte; }
    }


    public static class InvoiceTributo {
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


    public int getPuntoVenta() { return puntoVenta; }
    public void setPuntoVenta(int puntoVenta) { this.puntoVenta = puntoVenta; }

    public int getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(int tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public int getConcepto() { return concepto; }
    public void setConcepto(int concepto) { this.concepto = concepto; }

    public int getDocTipo() { return docTipo; }
    public void setDocTipo(int docTipo) { this.docTipo = docTipo; }

    public String getDocNro() { return docNro; }
    public void setDocNro(String docNro) { this.docNro = docNro; }

    public int getCondicionIvaReceptor() { return condicionIvaReceptor; }
    public void setCondicionIvaReceptor(int condicionIvaReceptor) { this.condicionIvaReceptor = condicionIvaReceptor; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getFchServDesde() { return fchServDesde; }
    public void setFchServDesde(String fchServDesde) { this.fchServDesde = fchServDesde; }

    public String getFchServHasta() { return fchServHasta; }
    public void setFchServHasta(String fchServHasta) { this.fchServHasta = fchServHasta; }

    public String getFchVtoPago() { return fchVtoPago; }
    public void setFchVtoPago(String fchVtoPago) { this.fchVtoPago = fchVtoPago; }

    public String getMonId() { return monId; }
    public void setMonId(String monId) { this.monId = monId; }

    public double getMonCotiz() { return monCotiz; }
    public void setMonCotiz(double monCotiz) { this.monCotiz = monCotiz; }

    public double getImpNeto() { return impNeto; }
    public void setImpNeto(double impNeto) { this.impNeto = impNeto; }

    public double getImpIVA() { return impIVA; }
    public void setImpIVA(double impIVA) { this.impIVA = impIVA; }

    public double getImpTrib() { return impTrib; }
    public void setImpTrib(double impTrib) { this.impTrib = impTrib; }

    public double getImpTotal() { return impTotal; }
    public void setImpTotal(double impTotal) { this.impTotal = impTotal; }

    public double getImpTotConc() { return impTotConc; }
    public void setImpTotConc(double impTotConc) { this.impTotConc = impTotConc; }

    public double getImpOpEx() { return impOpEx; }
    public void setImpOpEx(double impOpEx) { this.impOpEx = impOpEx; }

    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }

    public List<InvoiceTributo> getTributos() { return tributos; }
    public void setTributos(List<InvoiceTributo> tributos) { this.tributos = tributos; }

    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
}
