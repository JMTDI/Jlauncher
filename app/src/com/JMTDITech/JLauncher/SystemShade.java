package com.JMTDITech.JLauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * This phone's SystemUI has dedicated screens for notifications and quick settings instead of
 * a pull-down shade (the original launcher opened exactly these). Other phones won't have them.
 */
final class SystemShade {
    static final String NOTIFICATIONS =
            "com.android.systemui/com.android.systemui.ui.NotificationActivity";
    static final String TILES =
            "com.android.systemui/com.android.systemui.ui.TilesActivity";

    private SystemShade() {}

    /** Try to open the phone's own screen. Returns null on success, or a short reason. */
    static String tryOpen(Context c, boolean quick) {
        ComponentName cn = ComponentName.unflattenFromString(quick ? TILES : NOTIFICATIONS);
        if (cn == null) return "bad component";
        try {
            c.getPackageManager().getActivityInfo(cn, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return "not found on this phone";
        }
        try {
            c.startActivity(new Intent(Intent.ACTION_MAIN).setComponent(cn)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return null;
        } catch (SecurityException e) {
            return "the phone doesn't allow other apps to open it";
        } catch (RuntimeException e) {
            return "error: " + e.getClass().getSimpleName();
        }
    }
}
