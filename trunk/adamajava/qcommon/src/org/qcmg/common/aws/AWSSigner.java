package org.qcmg.common.aws;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.services.s3.model.Region;

import uk.co.lucasweb.aws.v4.signer.HttpRequest;
import uk.co.lucasweb.aws.v4.signer.Signer;
import uk.co.lucasweb.aws.v4.signer.Signer.Builder;
import uk.co.lucasweb.aws.v4.signer.credentials.AwsCredentials;

/**
 * Convenience methods for Amazon version 4 request signing.
 * 
 * @author conradL
 *
 */
public class AWSSigner {

    static final Logger logger = LoggerFactory.getLogger(AWSSigner.class);

    /**
     * {@code sha256Hex("")}
     */
    public static String SHA256_EMPTY = sha256Hex("");

    /**
     * By default even the short-lived credentials on EC2 etc. are valid for 12
     * hours. So in Lambda context it's definitely OK to have as static field.
     */
    static AWSCredentials credentials = AWSUtils.getAWSCredentials();

    /**
     * Modify the supplied HttpURLConnection by adding AWS version 4 signature
     * headers.
     * 
     * @param connection
     *            the connection to sign
     * @param region
     *            AWS region
     * @param contentSha256
     *            hex string SHA-256 digest of the request body. If there is no body
     *            (e.g with {@code GET,DELETE}) you must supply the digest of the
     *            empty string "", for convenience defined as AWSSigner.SHA256_EMPTY
     * @see <a href=
     *      "http://docs.aws.amazon.com/general/latest/gr/signature-version-4.html">
     *      AWS Signature Version 4</a>
     */
    public static void addSignature(AWSService service, Region region, HttpURLConnection connection,
            String contentSha256) {
        URI target = URI.create(connection.getURL().toString());
        String method = connection.getRequestMethod();
        Map<String, List<String>> headers = connection.getRequestProperties();
        Map<String, List<String>> signedHeaders = addSignatureMVH(service, region, target, method, contentSha256,
                headers);
        for (Map.Entry<String, List<String>> header : signedHeaders.entrySet()) {
            for (String value : header.getValue()) {
                connection.setRequestProperty(header.getKey(), value);
            }
        }
    }

    /**
     * Add AWS version 4 signature headers to the user-supplied headers. The result
     * is a set of headers valid for a {@code method} request to {@code target}.
     * 
     * @param target
     *            target URI
     * @param region
     *            AWS region
     * @param method
     *            GET/PUT/POST/DELETE
     * @param contentSha256
     *            hex string SHA-256 digest of the request body. If there is no body
     *            (e.g with {@code GET,DELETE}) you must supply the digest of the
     *            empty string "", for convenience defined as AWSSigner.SHA256_EMPTY
     * @param userHeaders
     *            user-supplied headers (can be null or empty Map)
     * @return updated set of headers including required AWS signature headers.
     * @return
     */
    public static Map<String, String> addSignature(AWSService service, Region region, URI target, String method,
            String contentSha256, Map<String, String> userHeaders) {
        Map<String, List<String>> mvh = new HashMap<String, List<String>>();
        for (Map.Entry<String, String> h : userHeaders.entrySet()) {
            mvh.put(h.getKey(), Arrays.asList(h.getValue()));
        }
        Map<String, List<String>> signedMVH = addSignatureMVH(service, region, target, method, contentSha256, mvh);
        Map<String, String> signedSimpleHeaders = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> h : signedMVH.entrySet()) {
            signedSimpleHeaders.put(h.getKey(), h.getValue().get(0));
        }
        return signedSimpleHeaders;
    }

    /**
     * Add AWS version 4 signature headers to the user-supplied headers. 'MVH'
     * stands for Multi-Valued Header, i.e. each value in the header is a List to
     * support multiple values for the same header key. The result is a set of
     * headers valid for a {@code method} request to {@code target}.
     * 
     * @param target
     *            target URI
     * @param region
     *            AWS region
     * @param method
     *            GET/PUT/POST/DELETE
     * @param contentSha256
     *            hex string SHA-256 digest of the request body. If there is no body
     *            (e.g with {@code GET,DELETE}) you must supply the digest of the
     *            empty string "", for convenience defined as AWSSigner.SHA256_EMPTY
     * @param userHeaders
     *            user-supplied headers (can be null or empty Map)
     * @return updated set of headers including required AWS signature headers.
     * 
     * @see <a href=
     *      "http://docs.aws.amazon.com/general/latest/gr/signature-version-4.html">
     *      AWS Signature Version 4</a>
     */
    public static Map<String, List<String>> addSignatureMVH(AWSService service, Region region, URI target,
            String method, String contentSha256, Map<String, List<String>> userHeaders) {

        logger.debug("addSignature({}, \"{}\", \"{}\", ...)", target, region, method);

        String now = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(Instant.now());
        String accessId = credentials.getAWSAccessKeyId();
        String secretKey = credentials.getAWSSecretKey();

        // The order we add headers for the signing process doesn't matter and
        // doesn't even have to match the order they're added in the request —
        // the canonicalization step in signing process takes care of that.
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Host", new ArrayList<String>(Arrays.asList(target.getHost())));
        headers.put("x-amz-date", new ArrayList<String>(Arrays.asList(now)));
        headers.put("x-amz-content-sha256", new ArrayList<String>(Arrays.asList(contentSha256)));
        if (credentials instanceof AWSSessionCredentials) {
            headers.put("x-amz-security-token",
                    new ArrayList<String>(Arrays.asList(((AWSSessionCredentials) credentials).getSessionToken())));
        }
        if (userHeaders != null) {
            userHeaders.forEach(headers::putIfAbsent);
        }
        Builder builder = Signer.builder().awsCredentials(new AwsCredentials(accessId, secretKey))
                .region(region.toString());
        for (Map.Entry<String, List<String>> h : headers.entrySet()) {
            for (String value : h.getValue()) {
                builder.header(h.getKey(), value);
            }
        }
        HttpRequest request = new HttpRequest(method, target);
        String signature = builder.build(request, service.toString(), contentSha256).getSignature();
        headers.put("Authorization", new ArrayList<String>(Arrays.asList(signature)));

        return headers;
    }

    public static String sha256Hex(String input) {
        return sha256Hex(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Return lower-case hexadecimal string representation of the SHA-256 digest of
     * {@code input}, as required for AWS request signing.
     * 
     * @param input
     *            the bytes to digest
     * @return the SHA-256 string.
     */
    public static String sha256Hex(byte[] input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // (should!) never get here:
            // https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html
            // says "(e)very implementation of the Java platform is required to
            // support ... SHA-256"
        }
        byte[] digestBytes = md.digest(input);
        return DatatypeConverter.printHexBinary(digestBytes).toLowerCase();
    }
}
