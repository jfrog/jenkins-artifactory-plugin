package org.jfrog.hudson.pipeline.docker.proxy;

import net.lightbody.bmp.mitm.CertificateInfo;
import net.lightbody.bmp.mitm.PemFileCertificateSource;
import net.lightbody.bmp.mitm.RootCertificateGenerator;

import java.io.File;
import java.util.Date;

/**
 * Created by romang on 8/29/16.
 */
public class CertManager {
    /**
     * The default algorithm to use when encrypting objects in PEM files (such as private keys).
     */
    private static final String DEFAULT_PEM_ENCRYPTION_ALGORITHM = "AES-128-CBC";
    public static String DEFAULT_RELATIVE_CERT_PATH = "secrets/jfrog/certs/jfrog.proxy.crt";
    public static String DEFAULT_RELATIVE_KEY_PATH = "secrets/jfrog/certs/jfrog.proxy.key";

    public static PemFileCertificateSource getCertificateSource(String proxyPublicKeyFilePath, String proxyPrivateKeyFilePath) {
        return new PemFileCertificateSource(
                new File(proxyPublicKeyFilePath),    // the PEM-encoded certificate file
                new File(proxyPrivateKeyFilePath),    // the PEM-encoded private key file
                DEFAULT_PEM_ENCRYPTION_ALGORITHM);
    }

    public static void createCertificateSource(String proxyPublicKeyFilePath, String proxyPrivateKeyFilePath) {

        CertificateInfo certificateInfo = new CertificateInfo()
                .commonName("localhost")
                .organization("Jfrog Ltd")
                .notBefore(new Date(System.currentTimeMillis() - 365L * 24L * 60L * 60L * 1000L))
                .notAfter(new Date(System.currentTimeMillis() + 365L * 24L * 60L * 60L * 1000L));

        RootCertificateGenerator rootCertificateGenerator = RootCertificateGenerator.builder().
                certificateInfo(certificateInfo).build();

        File certFile = new File(proxyPublicKeyFilePath);
        certFile.getParentFile().mkdirs();
        File keyFile = new File(proxyPrivateKeyFilePath);
        keyFile.getParentFile().mkdirs();

        rootCertificateGenerator.saveRootCertificateAsPemFile(certFile);
        rootCertificateGenerator.savePrivateKeyAsPemFile(keyFile, DEFAULT_PEM_ENCRYPTION_ALGORITHM);
    }

}
