package org.qcmg.protocol.s3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import org.qcmg.common.aws.AWSService;
import org.qcmg.common.aws.AWSSigner;
import org.qcmg.common.aws.AWSUtils;
import org.qcmg.common.aws.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.Region;

/**
 * A {@code java.net.URLConnection} that reads from s3:// URLs.
 * 
 * Write operations are not supported.
 * 
 * @author conradL
 *
 */
public class S3URLConnection extends HttpsURLConnection {

    static final Logger logger = LoggerFactory.getLogger(S3URLConnection.class);

    static final Region s3Region = Settings.getInstance().s3Region;

    private final HttpsURLConnection delegate;

    public S3URLConnection(URL url) throws IOException {

        super(AWSUtils.getHttpsEndpoint(s3Region, url.getHost(), url.getPath().substring(1)));

        logger.debug("S3URLConnection({})", url);

        /*
         * Have to use a delegate here because the implementation classes and packages
         * of HttpURLConnection in Java seem to be written and structured in such a way
         * as to be almost extension-proof (certainly I couldn't do it despite a day
         * trying...), see e.g. sun.net.www.protocol.https.HttpsURLConnectionImpl. The
         * boilerplate below is a consequence.
         * 
         * TIP in Eclipse: Source => Generate Delegate Methods...
         */
        delegate = (HttpsURLConnection) this.url.openConnection();
    }

    /*
     * Getters that make the connection require adding signature first; see
     * https://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html
     */

    public Object getContent() throws IOException {
        addSignature();
        return delegate.getContent();
    }

    public Object getContent(Class[] classes) throws IOException {
        addSignature();
        return delegate.getContent(classes);
    }

    public String getContentEncoding() {
        addSignature();
        return delegate.getContentEncoding();
    }

    public int getContentLength() {
        addSignature();
        return delegate.getContentLength();
    }

    public long getContentLengthLong() {
        addSignature();
        return delegate.getContentLengthLong();
    }

    public String getContentType() {
        addSignature();
        return delegate.getContentType();
    }

    public long getDate() {
        addSignature();
        return delegate.getDate();
    }

    public long getExpiration() {
        addSignature();
        return delegate.getExpiration();
    }

    public long getLastModified() {
        addSignature();
        return delegate.getLastModified();
    }

    public String getHeaderField(int n) {
        return delegate.getHeaderField(n);
    }

    public String getHeaderField(String name) {
        addSignature();
        return delegate.getHeaderField(name);
    }

    public Map<String, List<String>> getHeaderFields() {
        addSignature();
        return delegate.getHeaderFields();
    }

    public long getHeaderFieldDate(String name, long Default) {
        addSignature();
        return delegate.getHeaderFieldDate(name, Default);
    }

    public int getHeaderFieldInt(String name, int Default) {
        addSignature();
        return delegate.getHeaderFieldInt(name, Default);
    }

    public long getHeaderFieldLong(String name, long Default) {
        addSignature();
        return delegate.getHeaderFieldLong(name, Default);
    }

    public String getHeaderFieldKey(int n) {
        addSignature();
        return delegate.getHeaderFieldKey(n);
    }

    public InputStream getInputStream() throws IOException {
        addSignature();
        return delegate.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("This connection is read-only");
    }

    /*
     * All other delegate methods
     */

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public void connect() throws IOException {
        delegate.connect();
    }

    public boolean getInstanceFollowRedirects() {
        return delegate.getInstanceFollowRedirects();
    }

    public int getConnectTimeout() {
        return delegate.getConnectTimeout();
    }

    public void disconnect() {
        delegate.disconnect();
    }

    public InputStream getErrorStream() {
        return delegate.getErrorStream();
    }

    public boolean getDoInput() {
        return delegate.getDoInput();
    }

    public boolean getDoOutput() {
        return delegate.getDoOutput();
    }

    public boolean getAllowUserInteraction() {
        return delegate.getAllowUserInteraction();
    }

    public void addRequestProperty(String key, String value) {
        delegate.addRequestProperty(key, value);
    }

    public String getCipherSuite() {
        return delegate.getCipherSuite();
    }

    public HostnameVerifier getHostnameVerifier() {
        return delegate.getHostnameVerifier();
    }

    public long getIfModifiedSince() {
        return delegate.getIfModifiedSince();
    }

    public Certificate[] getLocalCertificates() {
        return delegate.getLocalCertificates();
    }

    public Principal getLocalPrincipal() {
        return delegate.getLocalPrincipal();
    }

    public boolean getDefaultUseCaches() {
        return delegate.getDefaultUseCaches();
    }

    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return delegate.getPeerPrincipal();
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public void setChunkedStreamingMode(int chunklen) {
        delegate.setChunkedStreamingMode(chunklen);
    }

    public void setConnectTimeout(int timeout) {
        delegate.setConnectTimeout(timeout);
    }

    public int getReadTimeout() {
        return delegate.getReadTimeout();
    }

    public String getRequestMethod() {
        return delegate.getRequestMethod();
    }

    public int getResponseCode() throws IOException {
        return delegate.getResponseCode();
    }

    public URL getURL() {
        return delegate.getURL();
    }

    public String getResponseMessage() throws IOException {
        return delegate.getResponseMessage();
    }

    public Permission getPermission() throws IOException {
        return delegate.getPermission();
    }

    public void setAllowUserInteraction(boolean allowuserinteraction) {
        delegate.setAllowUserInteraction(allowuserinteraction);
    }

    public boolean getUseCaches() {
        return delegate.getUseCaches();
    }

    public void setDefaultUseCaches(boolean defaultusecaches) {
        delegate.setDefaultUseCaches(defaultusecaches);
    }

    public String getRequestProperty(String key) {
        return delegate.getRequestProperty(key);
    }

    public Map<String, List<String>> getRequestProperties() {
        return delegate.getRequestProperties();
    }

    public SSLSocketFactory getSSLSocketFactory() {
        return delegate.getSSLSocketFactory();
    }

    public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
        return delegate.getServerCertificates();
    }

    public void setFixedLengthStreamingMode(int contentLength) {
        delegate.setFixedLengthStreamingMode(contentLength);
    }

    public void setFixedLengthStreamingMode(long contentLength) {
        delegate.setFixedLengthStreamingMode(contentLength);
    }

    public void setInstanceFollowRedirects(boolean followRedirects) {
        delegate.setInstanceFollowRedirects(followRedirects);
    }

    public void setRequestMethod(String method) throws ProtocolException {
        delegate.setRequestMethod(method);
    }

    public void setReadTimeout(int timeout) {
        delegate.setReadTimeout(timeout);
    }

    public boolean usingProxy() {
        return delegate.usingProxy();
    }

    public String toString() {
        return delegate.toString();
    }

    public void setDoInput(boolean doinput) {
        delegate.setDoInput(doinput);
    }

    public void setDoOutput(boolean dooutput) {
        throw new UnsupportedOperationException("This connection is read-only");
    }

    public void setHostnameVerifier(HostnameVerifier verifier) {
        delegate.setHostnameVerifier(verifier);
    }

    public void setUseCaches(boolean usecaches) {
        delegate.setUseCaches(usecaches);
    }

    public void setIfModifiedSince(long ifmodifiedsince) {
        delegate.setIfModifiedSince(ifmodifiedsince);
    }

    public void setRequestProperty(String key, String value) {
        delegate.setRequestProperty(key, value);
    }

    public void setSSLSocketFactory(SSLSocketFactory factory) {
        delegate.setSSLSocketFactory(factory);
    }

    /*
     * We assume that body of the request is empty, and provide
     * AWSSigner.SHA256_EMPTY for signing process. Supplying content in request body
     * will cause this method to return an INVALID connection that WILL FAIL AWS
     * authorization.
     */
    private void addSignature() {
        try {
            delegate.setRequestMethod(this.getRequestMethod());
            for (Map.Entry<String, List<String>> header : this.getRequestProperties().entrySet()) {
                for (String value : header.getValue()) {
                    delegate.setRequestProperty(header.getKey(), value);
                }
            }
            AWSSigner.addSignature(AWSService.S3, s3Region, delegate, AWSSigner.SHA256_EMPTY);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}
