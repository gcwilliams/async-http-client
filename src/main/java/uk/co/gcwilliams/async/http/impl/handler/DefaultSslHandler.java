package uk.co.gcwilliams.async.http.impl.handler;

import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * The default SSL handler
 *
 * @author : Gareth Williams
 **/
public class DefaultSslHandler extends SslHandler {

    private static final SSLContext SSL_CONTEXT;

    static {
        try {
            SSL_CONTEXT = SSLContext.getInstance("TLS");
            SSL_CONTEXT.init(null, null, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Unable to create SSL context", e);
        }
    }

    /**
     * Constructor
     *
     */
    public DefaultSslHandler() {
        this(null, -1);
    }

    /**
     * Constructor
     *
     * @param host the host
     * @param port the port
     */
    public DefaultSslHandler(String host, int port) {
        super(createSslEngine(host, port));
    }

    /**
     * Creates an SSL engine
     *
     * @param host the host
     * @param port the port
     * @return the SSL engine
     */
    private static SSLEngine createSslEngine(String host, int port) {
        SSLEngine sslEngine = host != null
            ? SSL_CONTEXT.createSSLEngine(host, port)
            : SSL_CONTEXT.createSSLEngine();
        sslEngine.setUseClientMode(true);
        return sslEngine;
    }
}
