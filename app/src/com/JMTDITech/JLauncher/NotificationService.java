package com.JMTDITech.JLauncher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Optional helper. While the JLauncher home screen is showing it takes the quick-menu key and the
 * Favorites key before other handlers, and sends you back home if the phone also opens its own app
 * for the menu key. It does not read screen content.
 */
public class NotificationService extends AccessibilityService {
    private static NotificationService instance;
    /** Uptime when the quick-menu key was last pressed on the home screen (0 = none pending). */
    private static volatile long menuKeyTime;
    private static final long UNDO_WINDOW_MS = 3000;

    static boolean isConnected() {
        return instance != null;
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public boolean onUnbind(Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        HomeActivity home = HomeActivity.visibleHome();
        if (home != null && HomeActivity.isFavoritesKey(event)) {
            if (down && event.getRepeatCount() == 0) home.openNotifications();
            return true;
        }
        if (home == null || !Prefs.isQuickMenuKey(this, event)) return false;
        if (down && event.getRepeatCount() == 0) {
            menuKeyTime = SystemClock.uptimeMillis();
            home.openApps();
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

        // The phone also opens its own app for this key (e.g. Contacts). Go back home;
        // JLauncher's apps list is still open there.
        long pressed = menuKeyTime;
        if (pressed != 0 && now - pressed <= UNDO_WINDOW_MS
                && Prefs.closePackages(this).contains(p)) {
            menuKeyTime = 0;
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
    }

    @Override public void onInterrupt() { }
}
