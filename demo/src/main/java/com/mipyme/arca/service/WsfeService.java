package com.mipyme.arca.service;

import com.mipyme.arca.dto.InvoiceRequest;
import com.mipyme.arca.dto.InvoiceResponse;
import com.mipyme.arca.model.ArcaConfig;
import com.mipyme.arca.model.InvoiceIndex;
import com.mipyme.arca.repository.ArcaConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class WsfeService {

    private static final String WSFE_HOMO_URL = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
    private static final String WSFE_PROD_URL = "https://servicios1.afip.gov.ar/wsfev1/service.asmx";

    private final WsaaService wsaaService;
    private final ArcaConfigRepository arcaConfigRepository;
    private final InvoiceIndexService invoiceIndexService;

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(WsfeService.class.getName());

    public WsfeService(WsaaService wsaaService, ArcaConfigRepository arcaConfigRepository,
                       InvoiceIndexService invoiceIndexService) {
        this.wsaaService = wsaaService;
        this.arcaConfigRepository = arcaConfigRepository;
        this.invoiceIndexService = invoiceIndexService;
    }


    public String getCuitEmisor() {
        return arcaConfigRepository.findTopByOrderByIdDesc()
                .map(ArcaConfig::getCuit)
                .orElseThrow(() -> new RuntimeException("No ARCA configuration found"));
    }

    private String getUrl() {
        ArcaConfig config = arcaConfigRepository.findTopByOrderByIdDesc().orElseThrow(() -> new RuntimeException("No configuration found"));
        return "PRODUCTION".equals(config.getEnvironment()) ? WSFE_PROD_URL : WSFE_HOMO_URL;
    }

    private String buildSoapRequest(String methodName, WsaaService.ArcaTokenResponse auth, String extraBody) {
        ArcaConfig config = arcaConfigRepository.findTopByOrderByIdDesc().orElseThrow(() -> new RuntimeException("No configuration found"));
        String cuit = config.getCuit();

        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<ar:" + methodName + ">" +
                "<ar:Auth>" +
                "<ar:Token>" + auth.token + "</ar:Token>" +
                "<ar:Sign>" + auth.sign + "</ar:Sign>" +
                "<ar:Cuit>" + cuit + "</ar:Cuit>" +
                "</ar:Auth>" +
                (extraBody != null ? extraBody : "") +
                "</ar:" + methodName + ">" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";
    }

    private String callSoap(String soapXml, String soapAction) throws Exception {
        String url = getUrl();
        logger.info("Calling WSFE (" + soapAction + ") at " + url);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "http://ar.gov.afip.dif.FEV1/" + soapAction)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(soapXml))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.severe("WSFE Error: " + response.statusCode() + " Body: " + response.body());
                throw new RuntimeException("Error llamando a WSFE: " + response.statusCode() + " " + response.body());
            }

            return response.body();
        } catch (java.net.http.HttpTimeoutException e) {
            logger.severe("WSFE Timeout: " + e.getMessage());
            throw new RuntimeException("Tiempo de espera agotado al conectar con AFIP (WSFE).");
        }
    }

    public String getLastVoucher(int ptoVta, int cbteTipo) throws Exception {
        WsaaService.ArcaTokenResponse auth = wsaaService.getCachedToken();
        if (auth == null) throw new RuntimeException("No hay un token válido.");

        String extra = "<ar:PtoVta>" + ptoVta + "</ar:PtoVta>" +
                       "<ar:CbteTipo>" + cbteTipo + "</ar:CbteTipo>";

        String soapXml = buildSoapRequest("FECompUltimoAutorizado", auth, extra);
        return callSoap(soapXml, "FECompUltimoAutorizado");
    }

    public String getParam(String methodName) throws Exception {
        WsaaService.ArcaTokenResponse auth = wsaaService.getCachedToken();
        if (auth == null) throw new RuntimeException("No hay un token válido.");

        String soapXml = buildSoapRequest(methodName, auth, null);
        return callSoap(soapXml, methodName);
    }

    public String getCotizacion(String monId) throws Exception {
        WsaaService.ArcaTokenResponse auth = wsaaService.getCachedToken();
        if (auth == null) throw new RuntimeException("No hay un token válido.");

        String extra = "<ar:MonId>" + monId + "</ar:MonId>";
        String soapXml = buildSoapRequest("FEParamGetCotizacion", auth, extra);
        return callSoap(soapXml, "FEParamGetCotizacion");
    }


    @Transactional
    public InvoiceResponse authorize(InvoiceRequest req) throws Exception {
        String cuitEmisor = getCuitEmisor();


        Optional<InvoiceIndex> existing = invoiceIndexService.findByIdempotencyKey(req, cuitEmisor);
        if (existing.isPresent()) {
            InvoiceIndex prev = existing.get();
            logger.info("Idempotent hit: returning existing invoice_index id=" + prev.getId()
                    + " resultado=" + prev.getResultado() + " cae=" + prev.getCae());
            InvoiceResponse cached = new InvoiceResponse();
            cached.setSuccess("A".equals(prev.getResultado()));
            cached.setResultado(prev.getResultado());
            cached.setCae(prev.getCae());
            cached.setCaeFchVto(prev.getCaeFchVto() != null ? prev.getCaeFchVto().toString().replace("-", "") : "");
            cached.setCbteDesde(prev.getCbteNro());
            cached.setCbteHasta(prev.getCbteNro());
            cached.setPuntoVenta(prev.getPtoVta());
            cached.setTipoComprobante(prev.getCbteTipo());
            cached.setCbteFecha(prev.getCbteFch() != null ? prev.getCbteFch().toString().replace("-", "") : "");
            cached.setInvoiceIndexId(prev.getId());
            return cached;
        }

        WsaaService.ArcaTokenResponse auth = wsaaService.getCachedToken();
        if (auth == null) throw new RuntimeException("No hay un token válido. Por favor renueve la autenticación ARCA.");


        String lastVoucherXml = getLastVoucher(req.getPuntoVenta(), req.getTipoComprobante());
        long lastCbteNro = parseLastCbteNro(lastVoucherXml);
        long nextCbteNro = lastCbteNro + 1;


        String cbteFch = req.getFecha().replace("-", "");


        Map<Integer, double[]> ivaMap = new HashMap<>();
        for (InvoiceRequest.InvoiceItem item : req.getItems()) {
            int ivaId = item.getIvaId();
            double[] accum = ivaMap.computeIfAbsent(ivaId, k -> new double[]{0, 0});
            accum[0] += item.getBaseImp();
            accum[1] += item.getIvaImporte();
        }


        StringBuilder ivaXml = new StringBuilder();
        ivaXml.append("<ar:Iva>");
        for (Map.Entry<Integer, double[]> entry : ivaMap.entrySet()) {
            ivaXml.append("<ar:AlicIva>");
            ivaXml.append("<ar:Id>").append(entry.getKey()).append("</ar:Id>");
            ivaXml.append("<ar:BaseImp>").append(String.format(Locale.US, "%.2f", entry.getValue()[0])).append("</ar:BaseImp>");
            ivaXml.append("<ar:Importe>").append(String.format(Locale.US, "%.2f", entry.getValue()[1])).append("</ar:Importe>");
            ivaXml.append("</ar:AlicIva>");
        }
        ivaXml.append("</ar:Iva>");


        StringBuilder tribXml = new StringBuilder();
        if (req.getTributos() != null && !req.getTributos().isEmpty()) {
            tribXml.append("<ar:Tributos>");
            for (InvoiceRequest.InvoiceTributo trib : req.getTributos()) {
                tribXml.append("<ar:Tributo>");
                tribXml.append("<ar:Id>").append(trib.getId()).append("</ar:Id>");
                tribXml.append("<ar:Desc>").append(escapeXml(trib.getDesc() != null ? trib.getDesc() : "")).append("</ar:Desc>");
                tribXml.append("<ar:BaseImp>").append(String.format(Locale.US, "%.2f", trib.getBaseImp())).append("</ar:BaseImp>");
                tribXml.append("<ar:Alic>").append(String.format(Locale.US, "%.2f", trib.getAlic())).append("</ar:Alic>");
                tribXml.append("<ar:Importe>").append(String.format(Locale.US, "%.2f", trib.getImporte())).append("</ar:Importe>");
                tribXml.append("</ar:Tributo>");
            }
            tribXml.append("</ar:Tributos>");
        }


        StringBuilder serviceDatesXml = new StringBuilder();
        int concepto = req.getConcepto();
        if (concepto == 2 || concepto == 3) {
            if (req.getFchServDesde() != null && !req.getFchServDesde().isEmpty()) {
                serviceDatesXml.append("<ar:FchServDesde>").append(req.getFchServDesde().replace("-", "")).append("</ar:FchServDesde>");
            }
            if (req.getFchServHasta() != null && !req.getFchServHasta().isEmpty()) {
                serviceDatesXml.append("<ar:FchServHasta>").append(req.getFchServHasta().replace("-", "")).append("</ar:FchServHasta>");
            }
            if (req.getFchVtoPago() != null && !req.getFchVtoPago().isEmpty()) {
                serviceDatesXml.append("<ar:FchVtoPago>").append(req.getFchVtoPago().replace("-", "")).append("</ar:FchVtoPago>");
            }
        }


        String docNro = req.getDocNro() != null && !req.getDocNro().isEmpty() ? req.getDocNro() : "0";

        String feCAEReqBody =
            "<ar:FeCAEReq>" +
                "<ar:FeCabReq>" +
                    "<ar:CantReg>1</ar:CantReg>" +
                    "<ar:PtoVta>" + req.getPuntoVenta() + "</ar:PtoVta>" +
                    "<ar:CbteTipo>" + req.getTipoComprobante() + "</ar:CbteTipo>" +
                "</ar:FeCabReq>" +
                "<ar:FeDetReq>" +
                    "<ar:FECAEDetRequest>" +
                        "<ar:Concepto>" + concepto + "</ar:Concepto>" +
                        "<ar:DocTipo>" + req.getDocTipo() + "</ar:DocTipo>" +
                        "<ar:DocNro>" + docNro + "</ar:DocNro>" +
                        "<ar:CbteDesde>" + nextCbteNro + "</ar:CbteDesde>" +
                        "<ar:CbteHasta>" + nextCbteNro + "</ar:CbteHasta>" +
                        "<ar:CbteFch>" + cbteFch + "</ar:CbteFch>" +
                        "<ar:ImpTotal>" + String.format(Locale.US, "%.2f", req.getImpTotal()) + "</ar:ImpTotal>" +
                        "<ar:ImpTotConc>" + String.format(Locale.US, "%.2f", req.getImpTotConc()) + "</ar:ImpTotConc>" +
                        "<ar:ImpNeto>" + String.format(Locale.US, "%.2f", req.getImpNeto()) + "</ar:ImpNeto>" +
                        "<ar:ImpOpEx>" + String.format(Locale.US, "%.2f", req.getImpOpEx()) + "</ar:ImpOpEx>" +
                        "<ar:ImpTrib>" + String.format(Locale.US, "%.2f", req.getImpTrib()) + "</ar:ImpTrib>" +
                        "<ar:ImpIVA>" + String.format(Locale.US, "%.2f", req.getImpIVA()) + "</ar:ImpIVA>" +
                        serviceDatesXml.toString() +
                        "<ar:MonId>" + req.getMonId() + "</ar:MonId>" +
                        "<ar:MonCotiz>" + String.format(Locale.US, "%.2f", req.getMonCotiz()) + "</ar:MonCotiz>" +
                        "<ar:CondicionIVAReceptorId>" + req.getCondicionIvaReceptor() + "</ar:CondicionIVAReceptorId>" +
                        tribXml.toString() +
                        ivaXml.toString() +
                    "</ar:FECAEDetRequest>" +
                "</ar:FeDetReq>" +
            "</ar:FeCAEReq>";

        String soapXml = buildSoapRequest("FECAESolicitar", auth, feCAEReqBody);


        String requestHash = InvoiceIndexService.sha256(soapXml);

        logger.info("Sending FECAESolicitar for PtoVta=" + req.getPuntoVenta() +
                     " CbteTipo=" + req.getTipoComprobante() + " CbteNro=" + nextCbteNro);

        String responseXml;
        try {
            responseXml = callSoap(soapXml, "FECAESolicitar");
        } catch (Exception e) {

            invoiceIndexService.recordEmitError(req, cuitEmisor,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), requestHash);
            throw e;
        }


        String responseHash = InvoiceIndexService.sha256(responseXml);


        InvoiceResponse resp = parseFECAEResponse(responseXml, req.getPuntoVenta(), req.getTipoComprobante(), cbteFch);


        try {
            InvoiceIndex saved = invoiceIndexService.persistEmission(req, resp, cuitEmisor, requestHash, responseHash);
            resp.setInvoiceIndexId(saved.getId());
        } catch (Exception e) {
            logger.severe("Error persisting invoice index: " + e.getMessage());

        }

        return resp;
    }


    public String consultarComprobante(int ptoVta, int cbteTipo, long cbteNro) throws Exception {
        WsaaService.ArcaTokenResponse auth = wsaaService.getCachedToken();
        if (auth == null) throw new RuntimeException("No hay un token válido.");

        String extra =
                "<ar:FeCompConsReq>" +
                    "<ar:CbteTipo>" + cbteTipo + "</ar:CbteTipo>" +
                    "<ar:CbteNro>" + cbteNro + "</ar:CbteNro>" +
                    "<ar:PtoVta>" + ptoVta + "</ar:PtoVta>" +
                "</ar:FeCompConsReq>";

        String soapXml = buildSoapRequest("FECompConsultar", auth, extra);
        return callSoap(soapXml, "FECompConsultar");
    }


    private long parseLastCbteNro(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList nodes = doc.getElementsByTagName("CbteNro");
            if (nodes.getLength() > 0) {
                return Long.parseLong(nodes.item(0).getTextContent().trim());
            }
        } catch (Exception e) {
            logger.warning("Could not parse last voucher number: " + e.getMessage());
        }
        return 0;
    }


    private InvoiceResponse parseFECAEResponse(String xml, int ptoVta, int cbteTipo, String cbteFch) {
        InvoiceResponse resp = new InvoiceResponse();
        resp.setPuntoVenta(ptoVta);
        resp.setTipoComprobante(cbteTipo);
        resp.setCbteFecha(cbteFch);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));


            NodeList faults = doc.getElementsByTagName("faultstring");
            if (faults.getLength() > 0) {
                resp.setSuccess(false);
                resp.setResultado("R");
                List<String> errores = new ArrayList<>();
                errores.add("SOAP Fault: " + faults.item(0).getTextContent());
                resp.setErrores(errores);
                return resp;
            }


            NodeList detNodes = doc.getElementsByTagName("FECAEDetResponse");
            if (detNodes.getLength() > 0) {
                Element det = (Element) detNodes.item(0);

                String resultado = getTagValue(det, "Resultado");
                resp.setResultado(resultado != null ? resultado : "R");
                resp.setSuccess("A".equals(resultado));

                String cae = getTagValue(det, "CAE");
                resp.setCae(cae != null ? cae : "");

                String caeFchVto = getTagValue(det, "CAEFchVto");
                resp.setCaeFchVto(caeFchVto != null ? caeFchVto : "");

                String cbteDesde = getTagValue(det, "CbteDesde");
                resp.setCbteDesde(cbteDesde != null ? Long.parseLong(cbteDesde) : 0);

                String cbteHasta = getTagValue(det, "CbteHasta");
                resp.setCbteHasta(cbteHasta != null ? Long.parseLong(cbteHasta) : 0);
            }


            List<String> observaciones = new ArrayList<>();
            NodeList obsNodes = doc.getElementsByTagName("Obs");
            for (int i = 0; i < obsNodes.getLength(); i++) {
                Element obs = (Element) obsNodes.item(i);
                String code = getTagValue(obs, "Code");
                String msg = getTagValue(obs, "Msg");
                observaciones.add((code != null ? code + ": " : "") + (msg != null ? msg : ""));
            }
            resp.setObservaciones(observaciones);


            List<String> errores = new ArrayList<>();
            NodeList errNodes = doc.getElementsByTagName("Err");
            for (int i = 0; i < errNodes.getLength(); i++) {
                Element err = (Element) errNodes.item(i);
                String code = getTagValue(err, "Code");
                String msg = getTagValue(err, "Msg");
                errores.add((code != null ? code + ": " : "") + (msg != null ? msg : ""));
            }
            resp.setErrores(errores);

        } catch (Exception e) {
            logger.severe("Error parsing FECAESolicitar response: " + e.getMessage());
            resp.setSuccess(false);
            resp.setResultado("R");
            List<String> errores = new ArrayList<>();
            errores.add("Error parseando respuesta de AFIP: " + e.getMessage());
            resp.setErrores(errores);
        }

        return resp;
    }


    private String getTagValue(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }


    private String escapeXml(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
