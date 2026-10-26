package com.JMTDITech.JLauncher;

import android.content.Intent;
import android.graphics.drawable.Drawable;

/** One launchable entry: an installed app, a standard intent target, or an internal screen. */
final class AppItem {
    final String id;
    final String label;
    final Drawable icon;
    final Intent intent;
    /** True for screens inside JLauncher (Tools, More, flashlight). */
    final boolean internal;

    AppItem(String id, String label, Drawable icon, Intent intent, boolean internal) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.intent = intent;
        this.internal = internal;
    }
}
