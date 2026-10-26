# JLauncher

A keypad-friendly Android home launcher modelled on a feature-phone launcher.
Package: `com.JMTDITech.JLauncher` · min SDK 26 · target SDK 34 · plain framework Views, Java 8, no AndroidX.

## Features
- **Home:** list mode ("Home Menu", date, carrier, numbered list) and a 2-column icon grid mode.
- **Keys on Home:** `0-9`, `*`, `#` open the dialer; long-press `#` toggles ringer/vibrate; BACK does nothing.
- **Quick menu** (Notifications, Quick settings, JLauncher settings), opened by `MENU`, `BOOKMARK`, `NOTIFICATION`, or any key you assign in Settings → *Quick menu key*.
  - Shown as a full-screen accessibility overlay (or a dialog; Settings → *Quick menu style*).
  - Notifications / Quick settings first try the phone's own SystemUI screens
    (`com.android.systemui.ui.NotificationActivity` / `TilesActivity`), then the standard shade via the accessibility service.
- **Tools** folder (editable list) and **More** folder (all installed apps; icon grid in grid layout, text list in list layout).
- **Settings** (the app's launcher icon): layout, edit lists, quick-menu key, overlay/dialog, apps to close after the menu key, accessibility, default-launcher request, reset.

## Accessibility service (optional, recommended)
Enable *Accessibility → JLauncher*. While the JLauncher home screen is showing it:
1. takes the quick-menu key before other handlers;
2. if the phone's firmware also opens its own app for that key (e.g. `com.android.contacts` on the tested phone),
   shows the menu as an overlay and sends you back Home;
3. can perform the standard "notifications / quick settings" actions and checks that something opened.

It reacts only to the quick-menu key and only while JLauncher is showing. It does not read screen content.
On Android 13+ you may need *App info → ⋮ → Allow restricted settings* first.

## Build (no Gradle)
Needs JDK 8+ (`javac`), `aapt`, `dx` (Debian/Ubuntu: `dalvik-exchange`), `zipalign`, `apksigner`, and an API 34 `android.jar`.

    sudo apt-get install aapt apksigner zipalign dalvik-exchange openjdk-21-jdk-headless
    curl -L -o android.jar https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar

    export KEYSTORE=/path/to/your.jks KS_PASS=... KEY_ALIAS=... KEY_PASS=...
    ./build.sh        # -> build/JLauncher.apk

Notes:
- `dx` cannot desugar lambdas, so the code uses anonymous classes and Java 8 bytecode only.
- If `dx` is somewhere else, set `DX=/path/to/dx`.
- Signing material is read from environment variables. Never commit a keystore.

## Status
Developed against one MediaTek feature phone (240x320, Android 14, production firmware).
Behaviour on other devices is untested.
