# Getting Started with WoW-Watchdog

This guide walks through installing WoW-Watchdog, configuring your server paths, validating notifications, and optionally setting up the Android companion app so you can monitor your server from your phone.

## Prerequisites

- **Windows 10/11** with **Windows PowerShell 5.1+**.
- **Administrator rights** if you install the Windows service.
- Optional: an **NTFY** server (or a public instance) if you want push notifications.
- Optional: an **Android 8.0+** device if you want to use the companion app.

These requirements mirror the supported platforms and features listed in the project overview.

## Installation (Recommended)

1. Download the latest installer from the **GitHub Releases** page.
2. Run the installer (administrator rights are required to install the service).
3. Launch **WoW Watchdog** from the desktop shortcut after installation completes.

WoW-Watchdog runs as a Windows service under the name **WoWWatchdog**.

## Portable Mode (No Installer)

If you prefer not to install the service, you can run the app in portable mode:

- Create a file named `portable.flag` in the same directory as the executable, **or**
- Start the app with the `-Portable` switch.

In portable mode, WoW-Watchdog stores data alongside the executable:

- `data\config.json` for configuration
- `data\secrets.json` for encrypted notification credentials
- `logs\` for logs
- `tools\` for helper tools

> Tip: Portable mode skips the automatic elevation prompt. Use it when you want to keep everything self-contained.

## First-Run Configuration

Open the app and configure these core settings:

1. **Server Paths**
   - Use the **Browse** buttons in the GUI to point to your MySQL, Authserver, and Worldserver executables or scripts.
2. **Expansion Label (Optional)**
   - Select the expansion for notification labeling; it doesn’t change monitoring behavior.
3. **Database Settings (Optional)**
   - Configure DB host/port/user/name if you want live player counts in the UI.
4. **NTFY Notifications (Optional)**
   - Enter your NTFY server and topic.
   - Choose **None**, **Basic**, or **Token** auth. Credentials are stored in `secrets.json` and encrypted on save.
   - Use the **Test** button to validate delivery.

Make sure to click **Save Configuration** so your paths and notification settings persist.

## Where Configuration and Logs Live

**Installed mode (default):**

- `%ProgramData%\WoWWatchdog\config.json`
- `%ProgramData%\WoWWatchdog\secrets.json`
- `%ProgramData%\WoWWatchdog\watchdog.log`
- `%ProgramData%\WoWWatchdog\backups\` (default backup location)

**Portable mode:**

- `data\config.json`
- `data\secrets.json`
- `logs\watchdog.log`
- `data\backups\` (default backup location)

## Enabling the REST API

The watchdog includes a built-in REST API that powers the Android companion app. It is disabled by default.

1. Open `config.json` (see [Where Configuration and Logs Live](#where-configuration-and-logs-live) for the path).
2. Set the `API` section to enabled:
   ```json
   "API": {
       "Enabled": true,
       "Port": 8099,
       "Bind": "+"
   }
   ```
   - **Port** &ndash; the TCP port the API listens on (default `8099`).
   - **Bind** &ndash; `"+"` listens on all interfaces; use a specific IP to restrict access.
3. Restart the watchdog service. On first start with the API enabled, an API key is generated automatically and saved to `api.secrets.json` next to your config file.
4. Copy the `ApiKey` value from `api.secrets.json` &mdash; you will need it for the Android app.

> **Security note:** The API uses key-based authentication. Keep your API key private. Failed authentication attempts trigger rate limiting and temporary IP lockouts after repeated failures.

## Android Companion App

The Android companion app gives you remote control of your WoW server stack from your phone. It communicates with the watchdog via the REST API described above.

### Requirements

- Android 8.0 or later (API level 26+)
- Network access to the machine running the watchdog (same LAN or VPN)
- The watchdog REST API must be enabled (see above)

### Building from Source

The Android project lives in the `android/` directory and uses a standard Gradle build.

1. Open the `android/` folder in **Android Studio** (Hedgehog or newer recommended).
2. Let Gradle sync and download dependencies.
3. Build the project: **Build > Make Project**, or from the command line:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```
4. Install the debug APK on your device or emulator:
   ```bash
   ./gradlew installDebug
   ```

> **Build requirements:** JDK 17+, Android SDK with API 35 platform installed.

### Connecting to Your Server

1. Open the app and navigate to **Settings**.
2. Under **Server Connection**, enter:
   - **Host / IP Address** &ndash; the IP or hostname of the machine running the watchdog (e.g., `192.168.1.100`).
   - **Port** &ndash; the API port (default `8099`).
   - **API Key** &ndash; the key from `api.secrets.json`.
3. Tap **Test Connection**. A green "Connected" message confirms the link.
4. Tap **Save Settings**.

### App Features

- **Dashboard** &ndash; live status cards for MySQL, Authserver, and Worldserver with at-a-glance health indicators.
- **Service Control** &ndash; start, stop, restart, or hold individual services. "Start All" and "Stop All" buttons are also available.
- **Console** &ndash; connect to the Worldserver RA console, send commands, and view output in real time.
- **Logs** &ndash; stream server log output directly on your device.
- **Settings** &ndash; configure the server connection, NTFY push notification details, UI theme (Default, Legion, WotLK, Cataclysm), and the status polling interval (3–60 seconds).

### Troubleshooting the Android App

- **"Connection failed"** &ndash; make sure the API is enabled in `config.json`, the watchdog service is running, and your device can reach the host on the configured port (check firewalls).
- **Stale status** &ndash; verify the polling interval in Settings; a very long interval means slower updates.
- **Build errors** &ndash; ensure you have JDK 17 and the Android SDK API 35 platform installed. Run `./gradlew --refresh-dependencies` if dependency resolution fails.

## Updating

Use the **Updates** tab in the GUI to pull the latest GitHub release and apply it safely. The update flow stops the service, installs the update, and restores service operation afterwards.

## Troubleshooting Quick Tips

- If the service won’t start, confirm the paths to your MySQL/Auth/Worldserver executables are correct.
- If notifications fail, verify the NTFY server URL and topic, then run a **Test** notification.
- For unexpected behavior, check `watchdog.log` and `crash.log` in your logs directory.
- For REST API issues, check the watchdog log for lines starting with `REST API`.

## Next Steps

- Review the full feature list and configuration details in the main README.
- Explore the **Tools** tab for optional companion utilities.
- Set up the Android companion app for remote monitoring on the go.
