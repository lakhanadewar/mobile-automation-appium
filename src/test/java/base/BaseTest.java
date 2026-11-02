package base;

import config.ConfigReader;
import driver.DriverFactory;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.time.Duration;

/**
 * Starts a fresh Appium session before every test and closes it afterwards.
 *
 *   -Dplatform = android or ios
 *   -DrunOn    = local or browserstack
 *   (defaults are in src/test/resources/config.properties)
 *
 * Example: mvn clean test -Dplatform=android -DrunOn=browserstack
 */
public class BaseTest {

    protected AppiumDriver driver;
    protected WebDriverWait wait;

    // From -Dplatform / -DrunOn if given, otherwise the defaults in config.properties
    protected static final String PLATFORM = ConfigReader.get("platform");
    protected static final String RUN_ON = ConfigReader.get("runOn");

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) throws MalformedURLException {
        driver = DriverFactory.createDriver(PLATFORM, RUN_ON);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        if (isBrowserStack()) {
            // Show the test name on the BrowserStack dashboard, e.g. "appNotificationShowsAndOpensApp [android]"
            String name = method.getName() + " [" + PLATFORM + "]";
            driver.executeScript("browserstack_executor: {\"action\": \"setSessionName\", "
                    + "\"arguments\": {\"name\":\"" + name + "\"}}");
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (driver == null) {
            return;
        }
        try {
            if (isBrowserStack()) {
                // Tell BrowserStack whether the test passed, so the dashboard shows the right status
                String status = result.isSuccess() ? "passed" : "failed";
                String reason = result.getThrowable() == null
                        ? "OK"
                        : result.getThrowable().getClass().getSimpleName();
                driver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", "
                        + "\"arguments\": {\"status\":\"" + status + "\", \"reason\": \"" + reason + "\"}}");
            }
        } finally {
            driver.quit();
        }
    }

    // ---------- small helpers ----------

    protected WebElement waitVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void tap(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    /**
     * Some commands (open notifications, press Android keys) exist only on AndroidDriver.
     * The driver really is an AndroidDriver when platform=android, so we can cast it.
     */
    protected AndroidDriver android() {
        return (AndroidDriver) driver;
    }

    /** Android-only: scrolls the first scrollable container until an element with this text is visible. */
    protected WebElement scrollToText(String text) {
        return driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))"
                        + ".scrollIntoView(new UiSelector().text(\"" + text + "\"))"));
    }

    protected static boolean isBrowserStack() {
        return RUN_ON.equalsIgnoreCase("browserstack");
    }
}
