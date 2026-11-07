# JLauncher

A keypad-friendly Android home launcher modelled on a feature-phone launcher.
Package: `com.JMTDITech.JLauncher` · min SDK 26 · target SDK 34 · plain framework Views, Java 8, no AndroidX.

## Features
- **Home:** wallpaper screen with carrier, clock and date; a "Notifications" bar at the bottom.
- **Keys on Home:**
  - `0-9`, `*`, `#` open the dialer.
  - Centre key opens the apps list; long-press opens the quick menu.
  - Soft-left or the Favorites key opens Notifications.
  - D-pad directions launch a shortcut; long-press one to choose the app.
  - BACK does nothing.
- **Apps:** numbered list or 3-column grid (toggle with MENU / SOFT_LEFT / BOOKMARK); digits `1-9` launch entries.
  Ends with a **More** entry and a **Tools** folder (editable).
- **More:** icon grid of every app not already in the apps menu. Long-press centre on an app for
  Add/Remove favorite, App info or Uninstall; favorites are listed first.
- **Notifications:** own screen (needs notification access). `*` clears the highlighted one, `#` clears all.
- **Quick menu:** Wi-Fi, Bluetooth, Hotspot (open system settings on Android 10+), Flashlight, Sound mode,
  Wallpapers (10 built-in, with preview) and Wallpaper from photos (with preview).
- **Settings** (the app's launcher icon): layout, edit lists, quick-menu key, accessibility, default-launcher request, reset.

## Accessibility service (optional, recommended)
Enable *Accessibility → JLauncher*. While the JLauncher home screen is showing it:
1. takes the quick-menu and Favorites keys before other handlers;
2. if the phone's firmware also opens its own app for the quick-menu key (e.g. `com.android.contacts`
   on the tested phone), sends you back Home.

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
