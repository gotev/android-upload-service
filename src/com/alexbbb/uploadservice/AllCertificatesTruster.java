package com.alexbbb.uploadservice;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * TrustManager that accepts all certificates.
 *
 * @author Alex Gotev
 */
public class AllCertificatesTruster implements TrustManager, X509TrustManager {

    private static final Logger LOGGER = Logger.getLogger(AllCertificatesTruster.class.getName());
    private static final String SSL_FAILED = "Unable to initialize the Trust Manager to trust all the "
                                           + "SSL certificates and HTTPS hosts.";

    @Override
    public final void checkClientTrusted(final X509Certificate[] xcs, final String string) throws CertificateException {
    }

    @Override
    public final void checkServerTrusted(final X509Certificate[] xcs, final String string) throws CertificateException {
    }

    @Override
    public final X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public static void trustAllSSLCertificates() {
        final TrustManager[] trustAllCerts = new TrustManager[] {new AllCertificatesTruster()};

        try {
            final SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            LOGGER.log(Level.INFO, SSL_FAILED);
        }
    }
}
