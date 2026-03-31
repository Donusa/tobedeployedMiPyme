package com.mipyme.arca.service;

import com.mipyme.arca.model.ArcaConfig;
import com.mipyme.arca.repository.ArcaConfigRepository;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

@Service
public class WsaaService {

    private static final Logger logger = LoggerFactory.getLogger(WsaaService.class);
    private static final String WSAA_HOMO_URL = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    private static final String WSAA_PROD_URL = "https://wsaa.afip.gov.ar/ws/services/LoginCms";

    private final ArcaConfigRepository arcaConfigRepository;
    private final TaSecurityService taSecurityService;

    public WsaaService(ArcaConfigRepository arcaConfigRepository, TaSecurityService taSecurityService) {
        this.arcaConfigRepository = arcaConfigRepository;
        this.taSecurityService = taSecurityService;
    }

    public ArcaTokenResponse authenticateWithP12(MultipartFile p12File, String password, String service) throws Exception {
        ArcaConfig config = arcaConfigRepository.findTopByOrderByIdDesc().orElse(null);
        if (config == null) {
            throw new RuntimeException("Configuración de ARCA no encontrada.");
        }


        String loginTicketRequestXml = createLoginTicketRequest(service);
        logger.info("TRA generado para servicio {}: {}", service, loginTicketRequestXml);


        byte[] cms = createCmsFromP12(loginTicketRequestXml, p12File.getInputStream(), password);
        String cmsBase64 = Base64.getEncoder().encodeToString(cms);
        logger.debug("CMS Base64 generado (longitud: {})", cmsBase64.length());


        String wsaaUrl = "PRODUCTION".equals(config.getEnvironment()) ? WSAA_PROD_URL : WSAA_HOMO_URL;
        logger.info("Conectando a WSAA en: {}", wsaaUrl);

        try {
            String soapResponse = callLoginCms(wsaaUrl, cmsBase64);
            logger.info("Respuesta WSAA: {}", soapResponse);

            ArcaTokenResponse response = parseLoginCmsResponse(soapResponse);


            LocalDateTime expiration = LocalDateTime.now().plusHours(10);
            taSecurityService.encryptAndSave(config, response.token, response.sign, expiration);

            return response;
        } catch (Exception e) {
            logger.error("Error crítico comunicando con WSAA.", e);
            throw e;
        }
    }




    public ArcaTokenResponse getCachedToken() {
        ArcaConfig config = arcaConfigRepository.findTopByOrderByIdDesc().orElse(null);
        if (config != null && config.getTokenExpiration() != null) {
            TaSecurityService.CachedToken secureToken = taSecurityService.getDecryptedToken(config);
            if (secureToken != null) {
                return new ArcaTokenResponse(secureToken.token, secureToken.sign);
            } else {


                logger.warn("Could not decrypt cached token. It may be corrupted or key changed.");
            }
        }
        return null;
    }

    private String createLoginTicketRequest(String service) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires"));

        LocalDateTime generationTime = now.minusMinutes(10);

        LocalDateTime expirationTime = now.plusMinutes(10);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String uniqueId = String.valueOf(System.currentTimeMillis() / 1000);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<loginTicketRequest version=\"1.0\">" +
                "<header>" +
                "<uniqueId>" + uniqueId + "</uniqueId>" +
                "<generationTime>" + generationTime.format(formatter) + "</generationTime>" +
                "<expirationTime>" + expirationTime.format(formatter) + "</expirationTime>" +
                "</header>" +
                "<service>" + service + "</service>" +
                "</loginTicketRequest>";
    }

    @SuppressWarnings("unchecked")
    private byte[] createCmsFromP12(String data, InputStream p12Stream, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(p12Stream, password.toCharArray());

        String alias = ks.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);


        if (cert.getPublicKey() instanceof RSAPublicKey && privateKey instanceof RSAPrivateKey) {
            RSAPublicKey pub = (RSAPublicKey) cert.getPublicKey();
            RSAPrivateKey priv = (RSAPrivateKey) privateKey;
            if (!pub.getModulus().equals(priv.getModulus())) {
                 logger.error("FATAL: La clave privada no coincide con la clave pública del certificado.");
                 throw new RuntimeException("Inconsistencia Criptográfica: El certificado almacenado no corresponde a la clave privada actual.");
            }
        }

        List<X509Certificate> certList = new ArrayList<>();
        certList.add(cert);
        Store<X509CertificateHolder> certs = new JcaCertStore(certList);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

        ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(privateKey);

        gen.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                        .build(sha1Signer, cert));

        gen.addCertificates(certs);

        CMSProcessableByteArray msg = new CMSProcessableByteArray(data.getBytes("UTF-8"));
        CMSSignedData signedData = gen.generate(msg, true);

        return signedData.getEncoded();
    }

    private String callLoginCms(String url, String cmsBase64) throws Exception {
        String soapXml =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://wsaa.view.sua.dvadac.desein.afip.gov\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<ser:loginCms>" +
                "<in0>" + cmsBase64 + "</in0>" +
                "</ser:loginCms>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(soapXml))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error llamando a WSAA: " + response.statusCode() + " " + response.body());
        }

        return response.body();
    }

    private ArcaTokenResponse parseLoginCmsResponse(String soapResponse) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(soapResponse)));

        String loginTicketResponseXml = doc.getElementsByTagName("loginCmsReturn").item(0).getTextContent();


        Document ticketDoc = builder.parse(new InputSource(new StringReader(loginTicketResponseXml)));

        String token = ticketDoc.getElementsByTagName("token").item(0).getTextContent();
        String sign = ticketDoc.getElementsByTagName("sign").item(0).getTextContent();

        return new ArcaTokenResponse(token, sign);
    }

    public static class ArcaTokenResponse {
        public String token;
        public String sign;

        public ArcaTokenResponse(String token, String sign) {
            this.token = token;
            this.sign = sign;
        }
    }
}
