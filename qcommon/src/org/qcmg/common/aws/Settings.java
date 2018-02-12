package org.qcmg.common.aws;

// This Region class is in s3 package but provides strings that work with all services.
import com.amazonaws.services.s3.model.Region;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Settings read from environment and configuration files.
 * 
 * @author conradL
 *
 */
public class Settings {

    public final String s3RegionKey = "aws.s3Region";

    public final Region s3Region;

    private static Settings instance = null;

    private Settings() {
        Config config = ConfigFactory.load();
        try {
            s3Region = Region.fromValue(config.getString(s3RegionKey));
        } catch (Exception e) {
            throw new IllegalArgumentException("Application is misconfigured: " + e.getMessage(), e);
        }
    }

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

}
