package com.JMTDITech.JLauncher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Optional helper. While the JLauncher home screen is showing it:
 *  - takes the quick-menu key before other handlers, and if the phone still opens its own app for
 *    that key (e.g. Contacts), sends you back home where the menu is open;
 *  - can perform the standard "open notifications / quick settings" actions on normal phones,
 *    and reports whether another window actually appeared so failures are not silent.
 * It does not read screen content.
 */
public class NotificationService extends AccessibilityService {
    private static NotificationService instance;
    private static volatile long lastWindowEvent;
    /** Uptime when the quick-menu key was last pressed on the home screen (0 = none pending). */
    private static volatile long menuKeyTime;
    private static final long UNDO_WINDOW_MS = 3000;
    private OverlayMenu overlay;

    static boolean isConnected() {
        return instance != null;
    }

    /** Uptime of the last window change that came from another app (e.g. SystemUI). */
    static long lastWindowEventTime() {
        return lastWindowEvent;
    }

    static boolean performShadeAction(boolean quick) {
        NotificationService s = instance;
        return s != null && s.performGlobalAction(
                quick ? GLOBAL_ACTION_QUICK_SETTINGS : GLOBAL_ACTION_NOTIFICATIONS);
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        overlay = new OverlayMenu(this);
    }

    @Override public boolean onUnbind(Intent intent) {
        if (overlay != null) overlay.dismiss();
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        OverlayMenu o = overlay;
        if (o != null && o.isShowing()) {
            if (Prefs.isQuickMenuKey(this, event)) {
                if (down && event.getRepeatCount() == 0) o.dismiss();
                return true;
            }
            return o.onKey(event);
        }
        HomeActivity home = HomeActivity.visibleHome();
        if (home == null || !Prefs.isQuickMenuKey(this, event)) return false;
        if (down && event.getRepeatCount() == 0) {
            menuKeyTime = SystemClock.uptimeMillis();
            if (o != null && Prefs.overlayMenu(this)) o.show();
            else home.toggleQuickMenu();
        }
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;
        String p = pkg.toString();
        long now = SystemClock.uptimeMillis();
        if (!p.equals(getPackageName())) lastWindowEvent = now;

        // Close the overlay if some other app takes over (e.g. an incoming call), once the
        // phone's own app for the menu key has had time to appear and be sent away.
        OverlayMenu o = overlay;
        if (o != null && o.isShowing() && !p.equals(getPackageName())
                && !p.equals("com.android.systemui")
                && now - o.shownAt() > OverlayMenu.GRACE_MS
                && !Prefs.closePackages(this).contains(p)) {
            o.dismiss();
        }

        // The phone also opens its own app for this key (e.g. Contacts). Go back home;
        // the quick menu is still open there.
        long pressed = menuKeyTime;
        if (pressed != 0 && now - pressed <= UNDO_WINDOW_MS
                && Prefs.closePackages(this).contains(p)) {
            menuKeyTime = 0;
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
    }

    @Override public void onInterrupt() { }
}
