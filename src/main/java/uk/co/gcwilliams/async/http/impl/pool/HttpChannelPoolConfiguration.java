package uk.co.gcwilliams.async.http.impl.pool;

/**
 * The HTTP channel pool handler configuration
 *
 * @author : Gareth Williams
 **/
public class HttpChannelPoolConfiguration {

    private final int maxInitialLineLength;

    private final int maxHeaderSize;

    private final int maxChunkSize;

    private final int initialBufferSize;

    private final int maxContentLength;

    private final boolean enableSni;

    public HttpChannelPoolConfiguration(
            int maxInitialLineLength,
            int maxHeaderSize,
            int maxChunkSize,
            int initialBufferSize,
            int maxContentLength,
            boolean enableSni) {
        this.maxInitialLineLength = maxInitialLineLength;
        this.maxHeaderSize = maxHeaderSize;
        this.maxChunkSize = maxChunkSize;
        this.initialBufferSize = initialBufferSize;
        this.maxContentLength = maxContentLength;
        this.enableSni = enableSni;
    }

    /**
     * Gets the max initial line length
     *
     * @return the max initial line length
     */
    public int getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    /**
     * Gets the max header size
     *
     * @return the max header size
     */
    public int getMaxHeaderSize() {
        return maxHeaderSize;
    }

    /**
     * Gets the max chunk size
     *
     * @return the max chunk size
     */
    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    /**
     * Gets the initial buffer size
     *
     * @return the initial buffer size
     */
    public int getInitialBufferSize() {
        return initialBufferSize;
    }

    /**
     * Gets the max content length
     *
     * @return the max content length
     */
    public int getMaxContentLength() {
        return maxContentLength;
    }

    /**
     * Determines if SNI should be enabled or not
     *
     * @return true if SNI should be enabled or not, false otherwise
     */
    public boolean isEnableSni() {
        return enableSni;
    }
}
