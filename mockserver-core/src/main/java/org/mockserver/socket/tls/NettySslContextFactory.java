package org.mockserver.socket.tls;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.mockserver.configuration.ConfigurationProperties;

import javax.net.ssl.SSLException;
import java.security.cert.X509Certificate;

import static org.mockserver.socket.tls.KeyAndCertificateFactory.keyAndCertificateFactory;

/**
 * @author jamesdbloom
 */
public class NettySslContextFactory {

    private static final NettySslContextFactory NETTY_SSL_CONTEXT_FACTORY = new NettySslContextFactory();
    private SslContext clientSslContext = null;
    private SslContext serverSslContext = null;

    private NettySslContextFactory() {
        System.setProperty("https.protocols", "SSLv3,TLSv1,TLSv1.1,TLSv1.2,TLSv1.3");
    }

    public static NettySslContextFactory nettySslContextFactory() {
        return NETTY_SSL_CONTEXT_FACTORY;
    }

    public synchronized SslContext createClientSslContext() {
        if (clientSslContext == null || ConfigurationProperties.rebuildKeyStore()) {
            try {
                clientSslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
                ConfigurationProperties.rebuildKeyStore(false);
            } catch (SSLException e) {
                throw new RuntimeException("Exception creating SSL context for client", e);
            }
        }
        return clientSslContext;
    }

    public synchronized SslContext createServerSslContext() {
        if (serverSslContext == null
            || !keyAndCertificateFactory().mockServerX509CertificateCreated()
            || !ConfigurationProperties.preventCertificateDynamicUpdate() && ConfigurationProperties.rebuildServerKeyStore()) {
            try {
                keyAndCertificateFactory().buildAndSaveCertificates();

                serverSslContext = SslContextBuilder.forServer(
                    keyAndCertificateFactory().mockServerPrivateKey(),
                    // do we need this password??
                    ConfigurationProperties.javaKeyStorePassword(),
                    new X509Certificate[]{
                        keyAndCertificateFactory().mockServerX509Certificate(),
                        keyAndCertificateFactory().mockServerCertificateAuthorityX509Certificate()
                    }
                ).build();
                ConfigurationProperties.rebuildServerKeyStore(false);
            } catch (Exception e) {
                throw new RuntimeException("Exception creating SSL context for server", e);
            }
        }
        return serverSslContext;
    }

}
