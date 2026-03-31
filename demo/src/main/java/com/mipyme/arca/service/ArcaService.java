package com.mipyme.arca.service;

import com.mipyme.arca.dto.CsrResponse;
import com.mipyme.arca.model.ArcaConfig;
import com.mipyme.arca.repository.ArcaConfigRepository;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.X509CertificateHolder;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ArcaService {

    private final ArcaConfigRepository arcaConfigRepository;

    private static final Map<String, KeyPair> tempKeys = new ConcurrentHashMap<>();

    public ArcaService(ArcaConfigRepository arcaConfigRepository) {
        this.arcaConfigRepository = arcaConfigRepository;
    }

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public CsrResponse generateCsrAndPrivateKey(String companyName, String cuit) throws Exception {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();


        tempKeys.put(cuit, keyPair);


        String subjectDn = String.format("CN=%s, O=%s, C=AR, SERIALNUMBER=CUIT %s", companyName, companyName, cuit);
        X500Name subject = new X500Name(subjectDn);


        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withRSA");
        ContentSigner signer = csBuilder.build(keyPair.getPrivate());
        PKCS10CertificationRequest csr = p10Builder.build(signer);


        StringWriter csrWriter = new StringWriter();
        JcaPEMWriter pemCsrWriter = new JcaPEMWriter(csrWriter);
        pemCsrWriter.writeObject(csr);
        pemCsrWriter.close();
        String csrPem = csrWriter.toString();


        ArcaConfig config = arcaConfigRepository.findTopByOrderByIdDesc().orElse(null);
        if (config == null) {
            config = new ArcaConfig();
        }
        config.setCompanyName(companyName);
        config.setCuit(cuit);
        arcaConfigRepository.save(config);

        return new CsrResponse(csrPem, null);
    }

    public byte[] generateP12(String certificateContent, String password) throws Exception {
        ArcaConfig config = arcaConfigRepository.findTopByOrderByIdDesc().orElse(null);
        if (config == null || config.getCuit() == null) {
            throw new RuntimeException("Configuración no encontrada. Reinicie el proceso.");
        }

        String cuit = config.getCuit();
        KeyPair keyPair = tempKeys.get(cuit);

        if (keyPair == null) {
            throw new RuntimeException("Clave privada no encontrada en memoria. La sesión ha expirado o el servidor se reinició. Por favor genere un nuevo CSR.");
        }

        PrivateKey privateKey = keyPair.getPrivate();


        PEMParser certParser = new PEMParser(new StringReader(certificateContent));
        Object certObject = certParser.readObject();
        certParser.close();

        X509CertificateHolder certHolder;
        if (certObject instanceof X509CertificateHolder) {
            certHolder = (X509CertificateHolder) certObject;
        } else {
             throw new RuntimeException("El contenido provisto no es un certificado válido (X509CertificateHolder).");
        }

        X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);


        KeyStore p12 = KeyStore.getInstance("PKCS12", "BC");
        p12.load(null, null);

        Certificate[] chain = new Certificate[]{certificate};
        p12.setKeyEntry("alias", privateKey, password.toCharArray(), chain);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p12.store(bos, password.toCharArray());


        tempKeys.remove(cuit);

        return bos.toByteArray();
    }



    public ArcaConfig getArcaConfig() {
        return arcaConfigRepository.findTopByOrderByIdDesc().orElse(null);
    }

    public void saveWizardState(Integer currentStep, Boolean wizardCompleted) {
        ArcaConfig config = arcaConfigRepository.findTopByOrderByIdDesc().orElse(null);
        if (config == null) {
            config = new ArcaConfig();
        }
        if (currentStep != null) {
            config.setCurrentStep(currentStep);
        }
        if (wizardCompleted != null) {
            config.setWizardCompleted(wizardCompleted);
        }
        arcaConfigRepository.save(config);
    }

    public void deleteConfig() {
        arcaConfigRepository.deleteAll();
    }

    public ArcaConfig updateConfig(ArcaConfig newConfig) {
        ArcaConfig config = arcaConfigRepository.findTopByOrderByIdDesc().orElse(null);
        if (config == null) {
            config = new ArcaConfig();
        }
        if (newConfig.getCompanyName() != null) {
            config.setCompanyName(newConfig.getCompanyName());
        }
        if (newConfig.getCuit() != null) {
            config.setCuit(newConfig.getCuit());
        }
        if (newConfig.getEnvironment() != null) {
            config.setEnvironment(newConfig.getEnvironment());
        }
        return arcaConfigRepository.save(config);
    }


}
