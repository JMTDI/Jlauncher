package com.JMTDITech.JLauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.AlarmClock;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Turns entry ids into launchable items.
 *   "std:<name>"        a standard app found through a system intent (Contacts, Camera, ...)
 *                       or one of JLauncher's own screens (tools, more, flashlight)
 *   "cmp:<pkg>/<class>" one specific installed activity
 * Entries that cannot be resolved on this device are skipped, like the original launcher.
 */
final class Entries {
    static final String[] STANDARD = {
            "contacts", "messages", "calls", "gallery", "camera", "settings",
            "calculator", "clock", "calendar", "music", "recorder",
            "flashlight", "tools", "more"
    };

    private Entries() {}

    static List<AppItem> resolveAll(Context c, List<String> ids) {
        List<AppItem> out = new ArrayList<AppItem>();
        for (String id : ids) {
            AppItem it = resolve(c, id);
            if (it != null) out.add(it);
        }
        return out;
    }

    static AppItem resolve(Context c, String id) {
        PackageManager pm = c.getPackageManager();
        if (id.startsWith("cmp:")) {
            ComponentName cn = ComponentName.unflattenFromString(id.substring(4));
            if (cn == null) return null;
            try {
                ActivityInfo ai = pm.getActivityInfo(cn, 0);
                Intent i = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(cn);
                return new AppItem(id, ai.loadLabel(pm).toString(), ai.loadIcon(pm), i, false);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        if (!id.startsWith("std:")) return null;
        String key = id.substring(4);

        if ("tools".equals(key)) {
            return new AppItem(id, "Tools", new GlyphDrawable("tools"),
                    new Intent(c, FolderActivity.class).putExtra(FolderActivity.EXTRA_FOLDER, "tools"),
                    true);
        }
        if ("more".equals(key)) {
            return new AppItem(id, "More", new GlyphDrawable("more"),
                    new Intent(c, FolderActivity.class).putExtra(FolderActivity.EXTRA_FOLDER, "more"),
                    true);
        }
        if ("flashlight".equals(key)) {
            return new AppItem(id, "Flashlight", new GlyphDrawable("flashlight"),
                    new Intent(c, TorchActivity.class), true);
        }

        Intent intent = standardIntent(key);
        if (intent == null) return null;
        ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (ri == null || ri.activityInfo == null || "android".equals(ri.activityInfo.packageName)) {
            // No default chosen (chooser would show): fall back to the first real match.
            List<ResolveInfo> all = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            ri = all.isEmpty() ? null : all.get(0);
        }
        if (ri == null) return null;
        Drawable icon = ri.loadIcon(pm);
        return new AppItem(id, ri.loadLabel(pm).toString(), icon, intent, false);
    }

    private static Intent main(String category) {
        return new Intent(Intent.ACTION_MAIN).addCategory(category);
    }

    private static Intent standardIntent(String key) {
        if ("contacts".equals(key)) return main(Intent.CATEGORY_APP_CONTACTS);
        if ("messages".equals(key)) return main(Intent.CATEGORY_APP_MESSAGING);
        if ("calls".equals(key)) return new Intent(Intent.ACTION_VIEW).setType(CallLog.Calls.CONTENT_TYPE);
        if ("gallery".equals(key)) return main(Intent.CATEGORY_APP_GALLERY);
        if ("camera".equals(key)) return new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        if ("settings".equals(key)) return new Intent(Settings.ACTION_SETTINGS);
        if ("calculator".equals(key)) return main(Intent.CATEGORY_APP_CALCULATOR);
        if ("clock".equals(key)) return new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        if ("calendar".equals(key)) return main(Intent.CATEGORY_APP_CALENDAR);
        if ("music".equals(key)) return main(Intent.CATEGORY_APP_MUSIC);
        if ("recorder".equals(key)) return new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        return null;
    }

    /** Every launcher-visible app, sorted by name. */
    static List<AppItem> installedApps(Context c) {
        final PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(main, 0);
        List<AppItem> out = new ArrayList<AppItem>();
        for (ResolveInfo ri : found) {
            ActivityInfo ai = ri.activityInfo;
            ComponentName cn = new ComponentName(ai.packageName, ai.name);
            Intent i = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(cn);
            boolean own = ai.packageName.equals(c.getPackageName());
            out.add(new AppItem("cmp:" + cn.flattenToString(),
                    ri.loadLabel(pm).toString(), ri.loadIcon(pm), i, own));
        }
        Collections.sort(out, new Comparator<AppItem>() {
            @Override public int compare(AppItem a, AppItem b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return out;
    }

    /** Launch an item the way the original does: new task for other apps, plain start for our own. */
    static void launch(Context c, AppItem it) {
        Intent i = new Intent(it.intent);
        if (!it.internal) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        }
        try {
            c.startActivity(i);
        } catch (android.content.ActivityNotFoundException e) {
            toast(c);
        } catch (SecurityException e) {
            toast(c);
        } catch (RuntimeException e) {
            android.util.Log.e("JLauncher", "Unable to launch " + i, e);
        }
    }

    private static void toast(Context c) {
        android.widget.Toast.makeText(c, "App isn't installed.", android.widget.Toast.LENGTH_SHORT).show();
    }
}
