package org.qcmg.common.aws;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.model.Region;

/**
 * Convenience methods for working with Amazon services.
 * 
 * @author conradL
 *
 */
public class AWSUtils {

    /**
     * Works with ~/.aws config files as well as on EC2 instances and in Lambda
     * functions with permissions granted through IAM roles.
     * 
     * @return first credentials in the default provider chain
     */
    public static AWSCredentials getAWSCredentials() {
        return DefaultAWSCredentialsProviderChain.getInstance().getCredentials();
    }

    /**
     * Return the https:// virtual-host style endpoint of an S3 object.
     *
     * @param s3Region
     *            AWS region of the S3 bucket
     * @param bucket
     *            bucket name
     * @param object
     *            name of the object in the bucket
     * @return https:// endpoint
     * @throws MalformedURLException
     */
    public static URL getHttpsEndpoint(Region s3Region, String bucket, String object) throws MalformedURLException {
        return URI.create("https://" + getS3Host(s3Region, bucket) + "/" + object).toURL();
    }

    /**
     * Return the virtual-host style hostname of an S3 bucket.
     *
     * @param s3Region
     *            AWS region of the S3 bucket
     * @param bucket
     *            bucket name
     * @return hostname
     */
    public static String getS3Host(Region s3Region, String bucket) {
        return bucket + ".s3-" + s3Region + ".amazonaws.com";
    }
}
