package org.qcmg.common.aws;

public enum AWSService {
    EXECUTE_API, GLACIER, S3;

    @Override
    public String toString() {
        switch (this) {
        case EXECUTE_API:
            return "execute-api";
        case GLACIER:
            return "glacier";
        case S3:
            return "s3";
        default:
            throw new IllegalArgumentException("toString() not implemented for this AWSService value");
        }
    }
}
