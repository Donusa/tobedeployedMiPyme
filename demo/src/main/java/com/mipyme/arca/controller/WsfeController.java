package com.mipyme.arca.controller;

import com.mipyme.arca.dto.InvoiceDetailResponse;
import com.mipyme.arca.dto.InvoiceRequest;
import com.mipyme.arca.dto.InvoiceResponse;
import com.mipyme.arca.model.InvoiceIndex;
import com.mipyme.arca.service.InvoiceIndexService;
import com.mipyme.arca.service.WsfeService;
import com.mipyme.company.PlanFeature;
import com.mipyme.company.PlanLimitService;
import com.mipyme.tenant.TenantContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/wsfe")
public class WsfeController {

    private final WsfeService wsfeService;
    private final InvoiceIndexService invoiceIndexService;
    private final PlanLimitService planLimitService;

    public WsfeController(WsfeService wsfeService, InvoiceIndexService invoiceIndexService,
                          PlanLimitService planLimitService) {
        this.wsfeService = wsfeService;
        this.invoiceIndexService = invoiceIndexService;
        this.planLimitService = planLimitService;
    }

    @GetMapping("/last-voucher")
    public ResponseEntity<?> getLastVoucher(@RequestParam int ptoVta, @RequestParam int cbteTipo) {
        try {
            return ResponseEntity.ok(wsfeService.getLastVoucher(ptoVta, cbteTipo));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/params/{type}")
    public ResponseEntity<?> getParams(@PathVariable String type) {
        try {
            String methodName;
            switch (type) {
                case "ptos-venta": methodName = "FEParamGetPtosVenta"; break;
                case "cbte-tipos": methodName = "FEParamGetTiposCbte"; break;
                case "doc-tipos": methodName = "FEParamGetTiposDoc"; break;
                case "iva": methodName = "FEParamGetTiposIva"; break;
                case "monedas": methodName = "FEParamGetTiposMonedas"; break;
                case "tributos": methodName = "FEParamGetTiposTributos"; break;
                case "condicion-iva": methodName = "FEParamGetCondicionIvaReceptor"; break;
                default: return ResponseEntity.badRequest().body("Unknown param type");
            }
            return ResponseEntity.ok(wsfeService.getParam(methodName));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/cotizacion")
    public ResponseEntity<?> getCotizacion(@RequestParam String monId) {
        try {
            return ResponseEntity.ok(wsfeService.getCotizacion(monId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }


    @PostMapping("/authorize")
    public ResponseEntity<?> authorizeInvoice(@RequestBody InvoiceRequest request) {
        planLimitService.assertPlanAccess(TenantContext.getCurrentTenant(), PlanFeature.FACTURACION_ARCA);
        try {
            InvoiceResponse response = wsfeService.authorize(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }




    @GetMapping("/invoices")
    public ResponseEntity<?> listInvoices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Short cbteTipo,
            @RequestParam(required = false) String resultado) {
        try {
            List<InvoiceIndex> invoices = invoiceIndexService.findByDateRange(from, to, cbteTipo, resultado);
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }


    @GetMapping("/invoices/{id}")
    public ResponseEntity<?> getInvoice(@PathVariable Long id) {
        try {
            return invoiceIndexService.findById(id)
                    .map(inv -> {
                        invoiceIndexService.recordView(id);
                        return ResponseEntity.ok((Object) inv);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }


    @GetMapping("/invoices/{id}/detail")
    public ResponseEntity<?> getInvoiceDetail(@PathVariable Long id) {
        try {
            InvoiceIndex index = invoiceIndexService.findById(id).orElse(null);
            if (index == null) return ResponseEntity.notFound().build();

            String responseXml = wsfeService.consultarComprobante(
                    index.getPtoVta(), index.getCbteTipo(), index.getCbteNro());

            InvoiceDetailResponse detail = parseFECompConsultarResponse(responseXml);
            invoiceIndexService.recordView(id);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }


    @GetMapping("/invoices/export")
    public ResponseEntity<?> exportInvoices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Short cbteTipo,
            @RequestParam(required = false) String resultado) {
        try {
            List<InvoiceIndex> invoices = invoiceIndexService.findByDateRange(from, to, cbteTipo, resultado);
            invoiceIndexService.recordExport("JSON " + from + " to " + to + ", " + invoices.size() + " records");
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }



    private InvoiceDetailResponse parseFECompConsultarResponse(String xml) {
        InvoiceDetailResponse detail = new InvoiceDetailResponse();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList resultNodes = doc.getElementsByTagName("ResultGet");
            if (resultNodes.getLength() > 0) {
                Element result = (Element) resultNodes.item(0);
                detail.setSuccess(true);
                detail.setConcepto(parseIntTag(result, "Concepto"));
                detail.setDocTipo(parseIntTag(result, "DocTipo"));
                detail.setDocNro(getTag(result, "DocNro"));
                detail.setCbteNro(parseLongTag(result, "CbteNro"));
                detail.setCbteFch(getTag(result, "CbteFch"));
                detail.setImpTotal(parseDoubleTag(result, "ImpTotal"));
                detail.setImpTotConc(parseDoubleTag(result, "ImpTotConc"));
                detail.setImpNeto(parseDoubleTag(result, "ImpNeto"));
                detail.setImpOpEx(parseDoubleTag(result, "ImpOpEx"));
                detail.setImpTrib(parseDoubleTag(result, "ImpTrib"));
                detail.setImpIVA(parseDoubleTag(result, "ImpIVA"));
                detail.setMonId(getTag(result, "MonId"));
                detail.setMonCotiz(parseDoubleTag(result, "MonCotiz"));
                detail.setCae(getTag(result, "CodAutorizacion"));
                detail.setCaeFchVto(getTag(result, "FchVto"));
                detail.setResultado(getTag(result, "Resultado"));
                detail.setFchServDesde(getTag(result, "FchServDesde"));
                detail.setFchServHasta(getTag(result, "FchServHasta"));
                detail.setFchVtoPago(getTag(result, "FchVtoPago"));
                detail.setFchProceso(getTag(result, "FchProceso"));
                detail.setPtoVta(parseIntTag(result, "PtoVta"));
                detail.setCbteTipo(parseIntTag(result, "CbteTipo"));


                List<InvoiceDetailResponse.IvaDetail> ivaList = new ArrayList<>();
                NodeList ivaNodes = result.getElementsByTagName("AlicIva");
                for (int i = 0; i < ivaNodes.getLength(); i++) {
                    Element ivaEl = (Element) ivaNodes.item(i);
                    InvoiceDetailResponse.IvaDetail iva = new InvoiceDetailResponse.IvaDetail();
                    iva.setId(parseIntTag(ivaEl, "Id"));
                    iva.setBaseImp(parseDoubleTag(ivaEl, "BaseImp"));
                    iva.setImporte(parseDoubleTag(ivaEl, "Importe"));
                    ivaList.add(iva);
                }
                detail.setIvaDetails(ivaList);


                List<InvoiceDetailResponse.TributoDetail> tribList = new ArrayList<>();
                NodeList tribNodes = result.getElementsByTagName("Tributo");
                for (int i = 0; i < tribNodes.getLength(); i++) {
                    Element tribEl = (Element) tribNodes.item(i);
                    InvoiceDetailResponse.TributoDetail trib = new InvoiceDetailResponse.TributoDetail();
                    trib.setId(parseIntTag(tribEl, "Id"));
                    trib.setDesc(getTag(tribEl, "Desc"));
                    trib.setBaseImp(parseDoubleTag(tribEl, "BaseImp"));
                    trib.setAlic(parseDoubleTag(tribEl, "Alic"));
                    trib.setImporte(parseDoubleTag(tribEl, "Importe"));
                    tribList.add(trib);
                }
                detail.setTributoDetails(tribList);
            }


            List<String> errores = new ArrayList<>();
            NodeList errNodes = doc.getElementsByTagName("Err");
            for (int i = 0; i < errNodes.getLength(); i++) {
                Element err = (Element) errNodes.item(i);
                String code = getTag(err, "Code");
                String msg = getTag(err, "Msg");
                errores.add((code != null ? code + ": " : "") + (msg != null ? msg : ""));
            }
            if (!errores.isEmpty()) {
                detail.setSuccess(false);
                detail.setErrores(errores);
            }

        } catch (Exception e) {
            detail.setSuccess(false);
            List<String> errores = new ArrayList<>();
            errores.add("Error parsing FECompConsultar response: " + e.getMessage());
            detail.setErrores(errores);
        }
        return detail;
    }

    private String getTag(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) return nodes.item(0).getTextContent().trim();
        return null;
    }

    private int parseIntTag(Element parent, String tagName) {
        String val = getTag(parent, tagName);
        return val != null ? Integer.parseInt(val) : 0;
    }

    private long parseLongTag(Element parent, String tagName) {
        String val = getTag(parent, tagName);
        return val != null ? Long.parseLong(val) : 0;
    }

    private double parseDoubleTag(Element parent, String tagName) {
        String val = getTag(parent, tagName);
        return val != null ? Double.parseDouble(val) : 0;
    }
}
