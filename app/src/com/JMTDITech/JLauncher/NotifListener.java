package com.JMTDITech.JLauncher;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Feeds JLauncher's own notifications screen; needs "Notification access" to be allowed. */
public class NotifListener extends NotificationListenerService {
    private static NotifListener instance;
    private static Runnable onChange;

    static boolean isConnected() {
        return instance != null;
    }

    static void setOnChange(Runnable r) {
        onChange = r;
    }

    static StatusBarNotification[] active() {
        NotifListener s = instance;
        if (s == null) return new StatusBarNotification[0];
        try {
            StatusBarNotification[] a = s.getActiveNotifications();
            return a == null ? new StatusBarNotification[0] : a;
        } catch (RuntimeException e) {
            return new StatusBarNotification[0];
        }
    }

    static void clear(String key) {
        NotifListener s = instance;
        if (s == null) return;
        try { s.cancelNotification(key); } catch (RuntimeException ignored) { }
    }

    static void clearAll() {
        NotifListener s = instance;
        if (s == null) return;
        try { s.cancelAllNotifications(); } catch (RuntimeException ignored) { }
    }

    private static void changed() {
        Runnable r = onChange;
        if (r != null) r.run();
    }

    @Override public void onListenerConnected() {
        instance = this;
        changed();
    }

    @Override public void onListenerDisconnected() {
        instance = null;
        changed();
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) { changed(); }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) { changed(); }
}
