# Appium + Appium MCP + BrowserStack: ApiDemos (Android)

This project automates the **ApiDemos** sample app with Appium (Java + TestNG). It runs on a local emulator or on real devices in **BrowserStack App Automate**, and you can also drive the app by chatting with an AI through **Appium MCP**.

```
appium demo/
├── apps/                      ← put ApiDemos-debug.apk here
├── mcp/
│   ├── capabilities.json      ← device presets used by Appium MCP
│   └── claude_desktop_config.example.json
├── src/test/java/base/BaseTest.java     ← session set-up (local or BrowserStack)
├── src/test/java/tests/ApiDemosTest.java ← 6 tests
├── pom.xml
└── testng.xml
```

---

## How to run

The `DriverFactory` (in `src/test/java/driver`) builds the right driver from two settings:

| Setting | Values | Default |
|---|---|---|
| `-Dplatform` | `android` or `ios` | `android` |
| `-DrunOn` | `local` or `browserstack` | `local` |

```
mvn clean test -Dplatform=android -DrunOn=local
mvn clean test -Dplatform=android -DrunOn=browserstack
mvn clean test -Dplatform=ios -DrunOn=browserstack
```

All defaults live in `src/test/resources/config.properties` (devices, OS versions, app file names, Appium URL, build name). Edit that file to change them permanently, or override any key for one run with `-D`, e.g. `-Dandroid.local.udid=RZCXC0BAZ5V` or `"-Dandroid.bs.device=Samsung Galaxy S23"`.

BrowserStack needs these environment variables (or put them in `.env`, which is git-ignored):

```
BROWSERSTACK_USERNAME
BROWSERSTACK_ACCESS_KEY
BROWSERSTACK_ANDROID_APP_ID   (bs://... from uploading the .apk)
BROWSERSTACK_IOS_APP_ID       (bs://... from uploading the .ipa)
```

`ApiDemosTest` is Android-only, so it's skipped when `-Dplatform=ios`. iOS needs its own app, such as the Sauce Labs My Demo App.

---

## Step 1. Install the prerequisites (Windows)

| Tool | Why | Check it works |
|---|---|---|
| **JDK 17** (Temurin is fine) | Runs the Java tests | `java -version` |
| **Maven** | Builds and runs the tests | `mvn -v` |
| **Node.js 22 LTS** | Appium 3 and Appium MCP are Node apps (Appium needs 20.19+, Appium MCP needs 22+) | `node -v` |
| **Android Studio** | Gives you the Android SDK, `adb` and the emulator | `adb version` |

Set the environment variables once (PowerShell, then **open a new terminal**):

```powershell
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot"
setx ANDROID_HOME "C:\Users\lakha\AppData\Local\Android\Sdk"
setx PATH "$env:PATH;C:\Users\lakha\AppData\Local\Android\Sdk\platform-tools;C:\Users\lakha\AppData\Local\Android\Sdk\emulator"
```

Adjust the JDK folder name to whatever you installed.

In Android Studio, open **Device Manager**, create a Pixel emulator (Android 13 or 14 image) and start it. Then:

```powershell
adb devices        # should list emulator-5554   device
```

## Step 2. Install Appium and the Android driver

```powershell
npm install -g appium
appium -v                                   # 3.x
appium driver install uiautomator2          # Android driver
appium driver doctor uiautomator2           # checks ANDROID_HOME, JAVA_HOME, adb...
appium                                      # starts the server on http://127.0.0.1:4723
```

Leave that terminal running. Since Appium 2 the server is just a core, and each platform driver (UiAutomator2 for Android, XCUITest for iOS) is installed separately.

**Optional but very useful:** install **Appium Inspector** (desktop app, from github.com/appium/appium-inspector/releases). Point it at `http://127.0.0.1:4723` with the capabilities from `mcp/capabilities.json` to see the element tree and copy locators.

## Step 3. Get the ApiDemos app

Download `ApiDemos-debug.apk` from **github.com/appium/android-apidemos/releases** and save it as:

```
C:\Users\lakha\GenAI Project\appium demo\apps\ApiDemos-debug.apk
```

Package: `io.appium.android.apis` Launch activity: `.ApiDemos`

## Step 4. Run the tests locally

With the emulator running and Appium started:

```powershell
cd "C:\Users\lakha\GenAI Project\appium demo"
mvn clean test
```

Useful options: `-Dandroid.local.udid=emulator-5554` to pick a device, `-Dappium.url=...` if the server lives elsewhere.

What the tests cover:

| Test | Concept it shows |
|---|---|
| `appLaunchesOnHomeScreen` | Session start, accessibility ID locator, explicit wait |
| `setWifiNameInPreferences` | Checkbox state, text input, dialog OK/Cancel, verifying saved data |
| `okCancelDialog` | Native Android dialogs are plain elements, not `switchTo().alert()` |
| `scrollToWebView` | Scrolling with `UiScrollable` (Android UIAutomator locator) |
| `dragAndDrop` | `mobile: dragGesture` |
| `longPressOpensMenu` | `mobile: longClickGesture` |

## Step 5. Run on BrowserStack

1. Sign up at browserstack.com (the free trial includes App Automate minutes).
2. Copy your **username** and **access key** from *App Automate → Access Key*.
3. Upload the APK (Windows 10/11 ships with `curl.exe`):

```powershell
curl.exe -u "YOUR_USERNAME:YOUR_ACCESS_KEY" -X POST "https://api-cloud.browserstack.com/app-automate/upload" -F "file=@apps/ApiDemos-debug.apk"
```

The response looks like `{"app_url":"bs://a1b2c3d4..."}`. That `bs://` value is your app ID. Uploads expire after 30 days.

4. Set the credentials for this terminal and run:

```powershell
$env:BROWSERSTACK_USERNAME   = "YOUR_USERNAME"
$env:BROWSERSTACK_ACCESS_KEY = "YOUR_ACCESS_KEY"
$env:BROWSERSTACK_ANDROID_APP_ID = "bs://a1b2c3d4..."

mvn clean test -Dplatform=android -DrunOn=browserstack
# choose another device:
mvn clean test -Dplatform=android -DrunOn=browserstack "-Dandroid.bs.device=Samsung Galaxy S23" "-Dandroid.bs.osVersion=13.0"
```

5. Open **app-automate.browserstack.com**. Each test is a session with video, screenshots, Appium logs, device logs and network logs. The pass/fail mark comes from the `setSessionStatus` call in `BaseTest.tearDown`.

No local Appium server or emulator is needed for this step, because BrowserStack runs Appium for you on its hub `https://hub-cloud.browserstack.com/wd/hub`.

**Alternative:** BrowserStack also offers the `browserstack-java-sdk` with a `browserstack.yml` file. It handles credentials, devices and parallel runs without code changes. This project uses plain `bstack:options` capabilities instead, so every setting is visible in the code.

## Step 6. Appium MCP (drive the app with AI)

**MCP (Model Context Protocol)** lets an AI assistant such as Claude Desktop, Claude Code or Cursor call tools. **Appium MCP** (`appium-mcp` on npm, from the Appium team) exposes Appium as tools: create session, find element, tap, type, scroll, screenshot, generate locators and so on. You describe what you want in plain English, and the AI performs the steps and can then write the Java test for you.

### Set it up in Claude Desktop

1. Open *Settings → Developer → Edit Config* (the file is `%APPDATA%\Claude\claude_desktop_config.json`).
2. Copy in the contents of `mcp/claude_desktop_config.example.json`, fix the paths and keys, save, then restart Claude Desktop.

On Windows, `npx` has to be launched through `cmd /c`, which is why the config uses `"command": "cmd"`.

For **Claude Code**: `claude mcp add appium-mcp -- npx -y appium-mcp@latest`
For **Cursor**: *Settings → MCP → Add new MCP Server*, with command `npx -y appium-mcp@latest`.

### Use it locally

Start the emulator first (Appium MCP starts its own Appium session, so you don't need `appium` running for this). Then try prompts like:

> Select the Android emulator and start a session with the ApiDemos app from my capabilities file.
> Tap Views, scroll to WebView and take a screenshot.
> Generate locators for every element on the current screen.
> Now write a Java TestNG test for what we just did, following the style of BaseTest.java.

### Use it on BrowserStack

Appium MCP can attach to any remote Appium server. Ask:

> Create an Appium session with remoteServerUrl `https://YOUR_USERNAME:YOUR_ACCESS_KEY@hub-cloud.browserstack.com/wd/hub` and capabilities: platformName Android, appium:automationName UiAutomator2, appium:deviceName "Google Pixel 7", appium:platformVersion "13.0", appium:app "bs://...". Then open Preference > 3. Preference dependencies.

The **BrowserStack MCP server** (`@browserstack/mcp-server`, already in the example config) is a separate tool. It handles BrowserStack-side jobs such as uploading apps, listing devices and opening live sessions, so the two work well together.

## Troubleshooting

| Problem | Fix |
|---|---|
| `Could not find a connected Android device` | Start the emulator, check `adb devices` |
| `ANDROID_HOME is not set` | Run the `setx` commands, then open a **new** terminal |
| `Connection refused 127.0.0.1:4723` | `appium` isn't running |
| `NoSuchElementException` | Check the locator in Appium Inspector. Screens differ slightly between Android versions |
| BrowserStack `[BROWSERSTACK_INVALID_APP_CAP]` | The `bs://` ID is wrong or the upload has expired, so upload again |
| Port 8200 in use (parallel local runs) | Give each device its own `appium:systemPort` |
