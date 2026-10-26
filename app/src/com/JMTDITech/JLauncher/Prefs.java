package com.JMTDITech.JLauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** All JLauncher settings. Lists are stored as newline-separated entry ids. */
final class Prefs {
    static final int LAYOUT_LIST = 1;
    static final int LAYOUT_GRID = 2;

    static final String KEY_LAYOUT = "layout_type";
    static final String KEY_HOME_LIST = "home_list";
    static final String KEY_HOME_GRID = "home_grid";
    static final String KEY_TOOLS = "tools_list";
    static final String KEY_MENU_KEY = "quick_menu_key";
    static final String KEY_CLOSE_PKGS = "close_after_menu_key";
    static final String KEY_OVERLAY = "overlay_menu";

    private Prefs() {}

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("jlauncher", Context.MODE_PRIVATE);
    }

    static int layout(Context c) {
        return sp(c).getInt(KEY_LAYOUT, LAYOUT_LIST) == LAYOUT_GRID ? LAYOUT_GRID : LAYOUT_LIST;
    }

    static void setLayout(Context c, int type) {
        sp(c).edit().putInt(KEY_LAYOUT, type).apply();
    }

    /** Keys that open the quick menu out of the box. Android has no KEYCODE_FAVORITE. */
    private static boolean isDefaultMenuKey(int code) {
        return code == KeyEvent.KEYCODE_MENU
                || code == KeyEvent.KEYCODE_BOOKMARK
                || code == KeyEvent.KEYCODE_NOTIFICATION;
    }

    /** True if this key event should open the quick menu (default keys or the learned key). */
    static boolean isQuickMenuKey(Context c, KeyEvent e) {
        int code = e.getKeyCode();
        if (isDefaultMenuKey(code)) return true;
        String s = sp(c).getString(KEY_MENU_KEY, null);
        if (s == null) return false;
        try {
            if (s.startsWith("kc:")) return code == Integer.parseInt(s.substring(3));
            if (s.startsWith("sc:")) {
                return code == KeyEvent.KEYCODE_UNKNOWN
                        && e.getScanCode() == Integer.parseInt(s.substring(3));
            }
        } catch (NumberFormatException ignored) {
            // corrupt value: treat as unset
        }
        return false;
    }

    /** Keys used for navigation or dialing cannot be reassigned. */
    static boolean isAssignable(KeyEvent e) {
        int k = e.getKeyCode();
        if (k >= KeyEvent.KEYCODE_0 && k <= KeyEvent.KEYCODE_9) return false;
        switch (k) {
            case KeyEvent.KEYCODE_STAR:
            case KeyEvent.KEYCODE_POUND:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_POWER:
                return false;
            default:
                return k != KeyEvent.KEYCODE_UNKNOWN || e.getScanCode() != 0;
        }
    }

    static void setMenuKey(Context c, KeyEvent e) {
        String v = e.getKeyCode() != KeyEvent.KEYCODE_UNKNOWN
                ? "kc:" + e.getKeyCode() : "sc:" + e.getScanCode();
        sp(c).edit().putString(KEY_MENU_KEY, v).apply();
    }

    static void clearMenuKey(Context c) {
        sp(c).edit().remove(KEY_MENU_KEY).apply();
    }

    static String describeMenuKey(Context c) {
        String s = sp(c).getString(KEY_MENU_KEY, null);
        String base = "MENU, BOOKMARK and NOTIFICATION keys";
        if (s == null) return "Default: " + base;
        try {
            if (s.startsWith("kc:")) {
                int k = Integer.parseInt(s.substring(3));
                return KeyEvent.keyCodeToString(k) + " (" + k + "), plus " + base;
            }
            if (s.startsWith("sc:")) {
                return "Unmapped key (scan code " + s.substring(3) + "), plus " + base;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return "Default: " + base;
    }

    /** Apps the phone itself opens when the menu key is pressed; JLauncher sends you home from them. */
    static List<String> closePackages(Context c) {
        String s = sp(c).getString(KEY_CLOSE_PKGS, "com.android.contacts");
        List<String> out = new ArrayList<String>();
        for (String part : s.split("[,\\s]+")) {
            if (part.length() > 0 && !out.contains(part)) out.add(part);
        }
        return out;
    }

    static void setClosePackages(Context c, String raw) {
        List<String> out = new ArrayList<String>();
        for (String part : raw.split("[,\\s]+")) {
            if (part.length() > 0 && !out.contains(part)) out.add(part);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < out.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(out.get(i));
        }
        sp(c).edit().putString(KEY_CLOSE_PKGS, sb.toString()).apply();
    }

    /** Quick menu as a full-screen overlay (needs the accessibility service) instead of a dialog. */
    static boolean overlayMenu(Context c) {
        return sp(c).getBoolean(KEY_OVERLAY, true);
    }

    static void setOverlayMenu(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_OVERLAY, on).apply();
    }

    static List<String> getList(Context c, String key) {
        String s = sp(c).getString(key, null);
        if (s == null) return defaults(key);
        List<String> out = new ArrayList<String>();
        if (s.length() == 0) return out;
        for (String part : s.split("\n")) {
            if (part.length() > 0) out.add(part);
        }
        return out;
    }

    static void setList(Context c, String key, List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(ids.get(i));
        }
        sp(c).edit().putString(key, sb.toString()).apply();
    }

    static List<String> defaults(String key) {
        if (KEY_HOME_GRID.equals(key)) {
            return new ArrayList<String>(Arrays.asList(
                    "std:contacts", "std:messages", "std:calls", "std:more"));
        }
        if (KEY_TOOLS.equals(key)) {
            return new ArrayList<String>(Arrays.asList(
                    "std:calculator", "std:clock", "std:flashlight", "std:calendar",
                    "std:music", "std:recorder"));
        }
        return new ArrayList<String>(Arrays.asList(
                "std:contacts", "std:messages", "std:calls", "std:gallery",
                "std:camera", "std:tools", "std:settings"));
    }

    static void resetAll(Context c) {
        sp(c).edit().clear().apply();
    }
}
