package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads settings from src/test/resources/config.properties.
 *
 * Order of priority:
 *   1. -Dkey=value on the command line (if given)
 *   2. the value in config.properties   (the default)
 */
public class ConfigReader {

    private static final Properties props = new Properties();

    // Runs once, the first time ConfigReader is used: loads the file into memory
    static {
        try (InputStream file = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (file == null) {
                throw new IllegalStateException("config.properties not found in src/test/resources");
            }
            props.load(file);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read config.properties", e);
        }
    }

    /** Returns the setting for this key. Empty string if it isn't set anywhere. */
    public static String get(String key) {
        String fromCommandLine = System.getProperty(key);
        if (fromCommandLine != null && !fromCommandLine.isBlank()) {
            return fromCommandLine.trim();
        }
        return props.getProperty(key, "").trim();
    }
}
