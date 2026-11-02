package driver;

import config.ConfigReader;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the right Appium driver.
 *
 *   platform: "android" or "ios"
 *   runOn:    "local" (your phone, emulator or simulator) or "browserstack" (cloud devices)
 *
 * Device names, app files and versions come from config.properties (see ConfigReader).
 * It always returns AppiumDriver, the parent of AndroidDriver and IOSDriver,
 * so the tests don't need to know which one they got.
 */
public class DriverFactory {

    private static final String BROWSERSTACK_URL = "https://hub-cloud.browserstack.com/wd/hub";

    public static AppiumDriver createDriver(String platform, String runOn) throws MalformedURLException {

        // 1. Where are we running? Pick the Appium server address.
        URL serverUrl;
        if (runOn.equalsIgnoreCase("local")) {
            serverUrl = URI.create(ConfigReader.get("appium.url")).toURL();
        } else if (runOn.equalsIgnoreCase("browserstack")) {
            serverUrl = URI.create(BROWSERSTACK_URL).toURL();
        } else {
            throw new IllegalArgumentException("Unknown runOn: " + runOn + " (use local or browserstack)");
        }

        // 2. Which platform? Build the matching options and driver.
        switch (platform.toLowerCase()) {

            case "android": {
                UiAutomator2Options options = new UiAutomator2Options();
                options.setNewCommandTimeout(Duration.ofSeconds(120));
                options.setAutoGrantPermissions(true);          // accept app permissions automatically

                if (runOn.equalsIgnoreCase("local")) {
                    // Physical phone or emulator connected to this computer (check with: adb devices)
                    options.setDeviceName("Android Device");
                    String udid = ConfigReader.get("android.local.udid");
                    if (!udid.isEmpty()) {
                        options.setUdid(udid);
                    }
                    String apk = ConfigReader.get("android.local.app");
                    options.setApp(Paths.get("apps", apk).toAbsolutePath().toString());
                } else {
                    // BrowserStack real device
                    options.setDeviceName(ConfigReader.get("android.bs.device"));
                    options.setPlatformVersion(ConfigReader.get("android.bs.osVersion"));
                    options.setApp(readEnv("BROWSERSTACK_ANDROID_APP_ID"));   // bs://...
                    options.setCapability("bstack:options", browserStackOptions());
                }

                return new AndroidDriver(serverUrl, options);
            }

            case "ios": {
                XCUITestOptions options = new XCUITestOptions();
                options.setNewCommandTimeout(Duration.ofSeconds(120));
                options.setAutoAcceptAlerts(true);              // accept iOS permission pop-ups automatically

                if (runOn.equalsIgnoreCase("local")) {
                    // Simulator (needs a .app) or real iPhone (needs a signed .ipa). Mac + Xcode only.
                    options.setDeviceName(ConfigReader.get("ios.local.device"));
                    String version = ConfigReader.get("ios.local.osVersion");
                    if (!version.isEmpty()) {
                        options.setPlatformVersion(version);
                    }
                    String udid = ConfigReader.get("ios.local.udid");   // only needed for a real iPhone
                    if (!udid.isEmpty()) {
                        options.setUdid(udid);
                    }
                    String app = ConfigReader.get("ios.local.app");
                    options.setApp(Paths.get("apps", app).toAbsolutePath().toString());
                } else {
                    // BrowserStack real iPhone
                    options.setDeviceName(ConfigReader.get("ios.bs.device"));
                    options.setPlatformVersion(ConfigReader.get("ios.bs.osVersion"));
                    options.setApp(readEnv("BROWSERSTACK_IOS_APP_ID"));       // bs://...
                    options.setCapability("bstack:options", browserStackOptions());
                }

                return new IOSDriver(serverUrl, options);
            }

            default:
                throw new IllegalArgumentException("Unknown platform: " + platform + " (use android or ios)");
        }
    }

    /** BrowserStack settings, the same for Android and iOS. */
    private static Map<String, Object> browserStackOptions() {
        Map<String, Object> bstack = new HashMap<>();
        bstack.put("userName", readEnv("BROWSERSTACK_USERNAME"));
        bstack.put("accessKey", readEnv("BROWSERSTACK_ACCESS_KEY"));
        bstack.put("projectName", ConfigReader.get("bs.projectName"));
        bstack.put("buildName", ConfigReader.get("bs.buildName"));
        bstack.put("debug", true);          // screenshot for every step
        bstack.put("networkLogs", true);
        bstack.put("deviceLogs", true);
        return bstack;
    }

    /** Secrets come from environment variables, never from config.properties. */
    private static String readEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is not set");
        }
        return value;
    }
}
