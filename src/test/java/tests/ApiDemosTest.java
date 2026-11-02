package tests;

import base.BaseTest;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
//import org.openqa.selenium.remote.RemoteWebElement;
//import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

public class ApiDemosTest extends BaseTest {

    private static final String PKG = "io.appium.android.apis";

    /** ApiDemos is an Android-only app, so skip these tests if someone runs with -Dplatform=ios. */
    @BeforeClass(alwaysRun = true)
    public void androidOnly() {
        if (!PLATFORM.equalsIgnoreCase("android")) {
            throw new SkipException("ApiDemosTest runs on Android only");
        }
    }

//    @Test(description = "App opens on the home list")
//    public void appLaunchesOnHomeScreen() {
//        WebElement views = waitVisible(AppiumBy.accessibilityId("Views"));
//        Assert.assertTrue(views.isDisplayed(), "Home screen did not load");
//    }
//
//    @Test(description = "Checkbox + text input + dialog: set WiFi name in Preference dependencies")
//    public void setWifiNameInPreferences() {
//        tap(AppiumBy.accessibilityId("Preference"));
//        tap(AppiumBy.accessibilityId("3. Preference dependencies"));
//
//        WebElement checkbox = waitVisible(By.id("android:id/checkbox"));
//        if (!"true".equals(checkbox.getAttribute("checked"))) {
//            checkbox.click();
//        }
//
//        By wifiSettings = By.xpath("//android.widget.TextView[@text='WiFi settings']");
//        tap(wifiSettings);
//        WebElement input = waitVisible(By.id("android:id/edit"));
//        input.clear();
//        input.sendKeys("MyHomeWiFi");
//        tap(By.id("android:id/button1"));                      // OK
//
//        // Re-open the dialog and check the value was saved
//        tap(wifiSettings);
//        Assert.assertEquals(waitVisible(By.id("android:id/edit")).getText(), "MyHomeWiFi");
//        tap(By.id("android:id/button2"));                      // Cancel
//    }
//
//    @Test(description = "Native Android dialog is handled as normal elements")
//    public void okCancelDialog() {
//        tap(AppiumBy.accessibilityId("App"));
//        tap(AppiumBy.accessibilityId("Alert Dialogs"));
//        tap(By.id(PKG + ":id/two_buttons"));
//
//        WebElement title = waitVisible(By.id("android:id/alertTitle"));
//        Assert.assertTrue(title.getText().startsWith("Lorem ipsum"), "Unexpected dialog title");
//        tap(By.id("android:id/button1"));                      // OK
//        Assert.assertTrue(driver.findElements(By.id("android:id/alertTitle")).isEmpty(), "Dialog still open");
//    }
//
//    @Test(description = "Scroll with UiScrollable until WebView is on screen")
//    public void scrollToWebView() {
//        tap(AppiumBy.accessibilityId("Views"));
//        waitVisible(AppiumBy.accessibilityId("Animation"));      // list has loaded
//        WebElement webView = scrollToText("WebView");
//        Assert.assertEquals(webView.getText(), "WebView");
//    }

    @Test(description = "App notification appears in the shade and tapping it opens the app")
    public void appNotificationShowsAndOpensApp() {
        tap(AppiumBy.accessibilityId("App"));
        tap(AppiumBy.accessibilityId("Notification"));
        tap(AppiumBy.accessibilityId("IncomingMessage"));

        tap(By.id(PKG + ":id/notify_app"));                    // "Show App Notification"
        allowNotificationsIfAsked();                           // Android 13+ may prompt on first notification

        android().openNotifications();                        // Android-only command
        By notificationTitle = By.xpath("//android.widget.TextView[@text='Joe']");
        WebElement notification;
        try {
            notification = waitVisible(notificationTitle);
        } catch (RuntimeException e) {
            android().pressKey(new KeyEvent(AndroidKey.BACK));    // don't leave the shade open for the next test
            throw new AssertionError("ApiDemos notification from 'Joe' did not appear in the shade", e);
        }
        Assert.assertTrue(notification.isDisplayed(), "Notification is not visible");

        notification.click();                                  // opens IncomingMessageView and closes the shade
        wait.until(d -> PKG.equals(currentPackage()));
        Assert.assertEquals(currentPackage(), PKG, "Tapping the notification did not open ApiDemos");
    }

    private String currentPackage() {
        return String.valueOf(driver.executeScript("mobile: getCurrentPackage"));
    }

    /** Taps "Allow" on the Android 13+ notification permission dialog if it shows up. */
    private void allowNotificationsIfAsked() {
        By allow = By.id("com.android.permissioncontroller:id/permission_allow_button");
        if (!driver.findElements(allow).isEmpty()) {
            driver.findElement(allow).click();
        }
    }

//    @Test(description = "Drag and drop using the UiAutomator2 mobile: dragGesture command")
//    public void dragAndDrop() {
//        tap(AppiumBy.accessibilityId("Views"));
//        waitVisible(AppiumBy.accessibilityId("Animation"));
//        scrollToText("Drag and Drop").click();
//
//        WebElement source = waitVisible(By.id(PKG + ":id/drag_dot_1"));
//        Rectangle target = driver.findElement(By.id(PKG + ":id/drag_dot_2")).getRect();
//
//        driver.executeScript("mobile: dragGesture", Map.of(
//                "elementId", ((RemoteWebElement) source).getId(),
//                "endX", target.getX() + target.getWidth() / 2,
//                "endY", target.getY() + target.getHeight() / 2,
//                "speed", 1500));
//
//        wait.until(ExpectedConditions.textToBe(By.id(PKG + ":id/drag_result_text"), "Dropped!"));
//    }

//    @Test(description = "Long press opens a context menu")
//    public void longPressOpensMenu() {
//        tap(AppiumBy.accessibilityId("Views"));
//        waitVisible(AppiumBy.accessibilityId("Animation"));
//        scrollToText("Expandable Lists").click();
//        tap(AppiumBy.accessibilityId("1. Custom Adapter"));
//
//        WebElement peopleNames = waitVisible(By.xpath("//android.widget.TextView[@text='People Names']"));
//        driver.executeScript("mobile: longClickGesture", Map.of(
//                "elementId", ((RemoteWebElement) peopleNames).getId(),
//                "duration", 1500));
//
//        Assert.assertTrue(waitVisible(By.xpath("//*[@text='Sample menu']")).isDisplayed(),
//                "Context menu did not appear");
//    }
}
