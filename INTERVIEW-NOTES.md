# Appium interview notes (Android + iOS)

Short, plain answers you can say out loud. The Android parts are all practised in this project. For iOS you need a Mac, so learn the concepts and the differences.

---

## 1. How Appium works

- **Client–server.** Your test (Java client) sends HTTP requests to the Appium server. The server passes them to a **driver**, and the driver talks to the device.
- **Protocol.** Appium speaks the **W3C WebDriver** protocol, the same as Selenium. That's why `findElement`, `click` and `sendKeys` look identical in both.
- **Drivers do the real work.**
  - Android: **UiAutomator2** driver. It installs a small helper app (the UiAutomator2 server) on the device, which uses Google's UiAutomator framework.
  - iOS: **XCUITest** driver. It builds and installs **WebDriverAgent (WDA)** on the device or simulator, which uses Apple's XCUITest framework.
- **Session.** Every test starts with "new session" plus **desired capabilities**, which tell Appium which device, which app and which driver to use.
- **No app changes needed.** Appium tests the same APK/IPA you ship. That's its main selling point over Espresso or XCUITest, which are white-box and live inside the app's codebase.

**Appium 1 vs 2 vs 3**
- Appium 2: drivers and plugins installed separately (`appium driver install uiautomator2`), base path changed from `/wd/hub` to `/`, and non-standard capabilities need the `appium:` prefix.
- Appium 3 (2025): mostly a clean-up. It needs Node 20.19+, drops old deprecated endpoints, and is stricter about W3C.

## 2. Capabilities you should know

| Capability | Meaning |
|---|---|
| `platformName` | Android / iOS |
| `appium:automationName` | UiAutomator2 / XCUITest / Espresso |
| `appium:deviceName`, `appium:udid` | Which device. `udid` is the precise one |
| `appium:platformVersion` | OS version |
| `appium:app` | Path or URL to APK/IPA (or `bs://` on BrowserStack) |
| `appium:appPackage` + `appium:appActivity` | Android: launch an already-installed app |
| `appium:bundleId` | iOS: launch an already-installed app |
| `appium:noReset` | `true` keeps app data between sessions |
| `appium:fullReset` | `true` uninstalls and reinstalls the app |
| `appium:newCommandTimeout` | Seconds Appium waits for the next command before ending the session |
| `appium:autoGrantPermissions` | Android: grant runtime permissions automatically |
| `appium:autoAcceptAlerts` | iOS: accept system permission pop-ups automatically |

**Q: noReset vs fullReset?** Default (both false) clears app data but keeps the install. `noReset=true` keeps everything, which is fast but can leak state between tests. `fullReset=true` reinstalls, which is the cleanest but slowest.

**Q: How do you find appPackage and appActivity?** Open the app, then run `adb shell dumpsys window | findstr mCurrentFocus`, or use `aapt dump badging app.apk`.

## 3. Locators

Priority order (fastest and most stable first):

1. **Accessibility ID**. Android `content-desc`, iOS `accessibilityIdentifier`. Works on **both** platforms, so it's the best for cross-platform tests.
2. **ID**. Android `resource-id` (e.g. `io.appium.android.apis:id/two_buttons`), iOS `name`.
3. **Platform-native queries**:
   - Android: `AppiumBy.androidUIAutomator("new UiSelector().text(\"OK\")")`
   - iOS: `AppiumBy.iOSNsPredicateString("label == 'OK'")` and `AppiumBy.iOSClassChain("**/XCUIElementTypeButton[`label == 'OK'`]")`
4. **XPath**. Last resort. It's slow because Appium has to dump the whole UI tree as XML, and it breaks easily. On iOS it's especially slow.

Inspect elements with **Appium Inspector** (or `driver.getPageSource()`).

**Element types**: Android uses class names like `android.widget.TextView`. iOS uses `XCUIElementTypeButton`, `XCUIElementTypeStaticText` and so on.

## 4. Waits and synchronisation

- **Implicit wait**: one global timeout for every `findElement`. Simple but hides problems, and mixing it with explicit waits gives odd timings.
- **Explicit wait** (`WebDriverWait` + `ExpectedConditions`): waits for one condition on one element. Preferred, and what this project uses.
- **Fluent wait**: explicit wait with custom polling interval and ignored exceptions.
- Never use `Thread.sleep` as a fix for flakiness.

## 5. Gestures

- **W3C Actions** (`PointerInput` + `Sequence`) work on both platforms. You build a touch: move to point, finger down, pause, move, finger up. This is how you do swipe, long press, pinch and zoom in modern Appium.
- **Driver "mobile:" commands** are simpler shortcuts:
  - Android UiAutomator2: `mobile: clickGesture`, `longClickGesture`, `dragGesture`, `swipeGesture`, `scrollGesture`, `pinchOpenGesture`
  - iOS XCUITest: `mobile: tap`, `touchAndHold`, `swipe`, `scroll`, `dragFromToForDuration`, `pinch`
- **TouchAction / MultiTouchAction** were removed in Java client 9. If an interviewer asks, say they're deprecated and replaced by W3C Actions.
- **Scrolling on Android**: `UiScrollable(...).scrollIntoView(...)` (used in this project) or `mobile: scrollGesture`. On iOS: `mobile: scroll` with `direction` or a predicate.

## 6. App types and contexts

- **Native**: built with Android/iOS SDKs. Context is `NATIVE_APP`.
- **Mobile web**: a site in Chrome/Safari. Set `browserName` instead of `app`.
- **Hybrid**: native shell with WebViews inside. You switch context:

```java
Set<String> contexts = driver.getContextHandles();  // NATIVE_APP, WEBVIEW_io.appium.android.apis
driver.context("WEBVIEW_io.appium.android.apis");
// now use normal web locators (CSS, etc.)
driver.context("NATIVE_APP");
```

- Android: the app must enable WebView debugging (`setWebContentsDebuggingEnabled(true)`), and Appium uses a matching **Chromedriver**.
- iOS: Appium talks to WebViews through the **Safari/WebKit remote debugger**. The app needs `isInspectable = true` from iOS 16.4.

## 7. Android vs iOS: the differences interviewers ask about

| Topic | Android | iOS |
|---|---|---|
| Driver | UiAutomator2 (or Espresso) | XCUITest |
| Helper on device | UiAutomator2 server APK | WebDriverAgent (WDA) |
| Host machine | Windows, Mac or Linux | **macOS only** (needs Xcode) |
| App file | `.apk` / `.aab` | `.app` (simulator) / `.ipa` (real device) |
| Virtual device | Emulator (real Android OS) | Simulator (runs on the Mac's CPU, not real iOS hardware) |
| Real device setup | Enable USB debugging, `adb devices` | Apple developer account, **code signing** and provisioning profile for WDA, `xcodeTeamId` |
| Device tool | `adb` | `xcrun simctl`, `idevice*` tools, Xcode |
| Launch installed app | `appPackage` + `appActivity` | `bundleId` |
| Native locators | UiAutomator selector | Predicate string, class chain |
| Back button | `driver.navigate().back()` or `pressKey(AndroidKey.BACK)` | No hardware back. Tap the nav bar back button |
| Permissions pop-up | `autoGrantPermissions` | `autoAcceptAlerts`, or `driver.switchTo().alert()` |
| Hide keyboard | `driver.hideKeyboard()` | Often tap "Done" or outside the field |
| Parallel ports | `appium:systemPort` per device | `appium:wdaLocalPort` per device |

**Q: Why is iOS harder to set up?** Real iOS devices need WDA signed with your Apple team ID and a provisioning profile, and everything has to run on a Mac with Xcode. Clouds like BrowserStack remove that pain, because their devices are already set up.

**Q: Emulator vs simulator?** An Android emulator virtualises the hardware and runs the real OS. An iOS simulator just runs iOS frameworks on the Mac, so things like the camera, push notifications and performance aren't realistic.

**Q: Emulator/simulator vs real device?** Virtual devices are cheap and fast for development and CI. Real devices are needed for performance, battery, sensors, OEM skins (Samsung, Xiaomi) and network conditions.

## 8. Framework design

- **Page Object Model**: one class per screen holding locators and actions. Tests read like steps. With `@AndroidFindBy` / `@iOSXCUITFindBy` and `AppiumFieldDecorator`, one page class can hold locators for both platforms.
- **Base test** (like `BaseTest.java` here): starts and stops the driver, reads config, hooks into reporting.
- **Config-driven**: device, platform and environment come from properties, YAML or `-D` flags, never hard-coded. Same code runs locally and on the cloud (as `-Drun=browserstack` does here).
- **Data-driven**: TestNG `@DataProvider`, or Excel/JSON files.
- **Reporting**: Extent Reports or Allure. Take a screenshot on failure in a TestNG listener.
- **Parallel runs**: keep the driver in a `ThreadLocal<AndroidDriver>` so each thread has its own session. Locally, each device needs its own `udid`, `systemPort` (Android) or `wdaLocalPort` (iOS). On a cloud, just raise TestNG's `thread-count`, within your plan's parallel limit.
- **CI/CD**: Jenkins or GitHub Actions runs `mvn test` against BrowserStack. Upload the new build first, then pass the `bs://` ID in as a variable.

## 9. Cloud testing with BrowserStack

- Upload the app via REST API, get a `bs://` app ID, put it in `appium:app`.
- Point the driver at `https://hub-cloud.browserstack.com/wd/hub` with credentials in `bstack:options`.
- Pick devices with `deviceName` + `platformVersion`.
- **Session status**: BrowserStack doesn't know if your assertion failed, so you mark it with `browserstack_executor: setSessionStatus`.
- **BrowserStack Local**: a tunnel so cloud devices can reach your internal or staging servers. Set `local: true` and run the Local binary or SDK.
- Debugging aids: video, screenshots per command (`debug: true`), network logs, device logs, Appium logs.
- Other clouds: Sauce Labs, LambdaTest, AWS Device Farm, Firebase Test Lab (Android). Same idea, different capability names.

## 10. Flaky tests: causes and fixes

| Cause | Fix |
|---|---|
| Element not ready | Explicit waits, not sleeps |
| Animations | Turn off animations in developer options (or `appium:disableWindowAnimation`) |
| State left from earlier tests | Reset strategy, independent tests, fresh session per test |
| Fragile XPath | Ask developers to add accessibility IDs |
| Keyboard covering elements | `hideKeyboard()` |
| Device differences | Scroll into view, don't rely on coordinates |
| Pop-ups (permissions, rate-us) | Auto-grant capabilities, a pop-up handler |

## 11. AI and MCP (newer topic, good to mention)

- **MCP (Model Context Protocol)** is an open standard that lets AI assistants call external tools.
- **Appium MCP** exposes Appium actions (sessions, finding elements, gestures, screenshots, locator generation) as MCP tools. An AI can explore an app from plain-English instructions and then produce test code.
- Where it helps: faster exploration of a new app, generating locators and first-draft tests, debugging a failing step. Where it doesn't replace you: framework design, assertions that reflect real requirements, and reviewing the generated code.

## 12. Quick-fire questions

1. **What is Appium?** Open-source, cross-platform tool for automating native, hybrid and mobile web apps using the WebDriver protocol.
2. **Does Appium need the app's source code?** No. It's black-box.
3. **Which languages?** Any with a WebDriver client: Java, Python, JS, C#, Ruby.
4. **What is the default port?** 4723.
5. **How do you get page source?** `driver.getPageSource()`.
6. **How do you take a screenshot?** `driver.getScreenshotAs(OutputType.FILE)`.
7. **How do you put the app in the background?** `driver.runAppInBackground(Duration.ofSeconds(5))`.
8. **How do you switch apps on Android?** `driver.activateApp("com.android.settings")`, then back with `activateApp` again.
9. **How do you check an app is installed?** `driver.isAppInstalled("io.appium.android.apis")`.
10. **How do you handle a native Android dialog?** Find its buttons like any element (`android:id/button1`). `switchTo().alert()` only works for iOS system alerts and web alerts.
11. **How do you rotate the device?** `driver.rotate(ScreenOrientation.LANDSCAPE)`.
12. **How do you read toast messages on Android?** UiAutomator2 can find them with XPath `//android.widget.Toast`.
13. **What is the Appium Inspector?** A GUI to start a session, view the element tree and try out locators.
14. **What is `appium driver doctor`?** Checks that a driver's dependencies (SDK, Java, Xcode) are set up properly.
15. **Espresso driver vs UiAutomator2?** Espresso is faster and syncs with the UI thread automatically, but needs to be built against the app and can't leave the app. UiAutomator2 is black-box and can work across apps and system UI.
