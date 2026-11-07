package com.JMTDITech.JLauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Home screen. List mode: "Home Menu", date, carrier, numbered list.
 * Grid mode: carrier plus a 2-column icon grid.
 * Keys: 0-9 / * / # open the dialer; long-press # toggles ringer/vibrate;
 * the quick-menu key (MENU / BOOKMARK / NOTIFICATION, or a key you assign) opens a small menu;
 * BACK does nothing here.
 */
public class HomeActivity extends Activity {
    private TextView time;
    private TextView date;
    private TextView carrier;
    private boolean poundLong;
    private boolean dpadLong;
    private boolean centerLong;
    private boolean receiverRegistered;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateHeader(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xCC0B0D10);
        getWindow().setNavigationBarColor(UI.NAVY_BOTTOM);
        setContentView(buildContent());
        updateHeader();
    }

    private TextView overlayText(int sp, int gravity) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(0xFFFFFFFF);
        t.setShadowLayer(6f, 0f, 0f, 0xFF000000);
        t.setSingleLine(true);
        t.setGravity(gravity);
        t.setPadding(UI.dp(this, 6), 0, UI.dp(this, 6), 0);
        return t;
    }

    private View buildContent() {
        FrameLayout root = new FrameLayout(this);

        carrier = overlayText(14, Gravity.END);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        cp.topMargin = UI.dp(this, 6);
        cp.rightMargin = UI.dp(this, 12);
        root.addView(carrier, cp);

        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        clock.setGravity(Gravity.END);
        time = overlayText(34, Gravity.END);
        date = overlayText(18, Gravity.END);
        clock.addView(time);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = UI.dp(this, 4);
        clock.addView(date, dlp);
        FrameLayout.LayoutParams kp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        kp.bottomMargin = UI.dp(this, 46);
        kp.rightMargin = UI.dp(this, 6);
        root.addView(clock, kp);

        LinearLayout bar = UI.glyphBar(this, "Notifications", "gridicon", true, true);
        bar.findViewWithTag("text").setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, NotificationsActivity.class));
            }
        });
        bar.findViewWithTag("glyph").setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openApps(); }
        });
        bar.findViewWithTag("text").setFocusable(false);
        bar.findViewWithTag("glyph").setFocusable(false);
        root.addView(bar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 38), Gravity.BOTTOM));
        return root;
    }

    /** The Favorites key arrives as keycode 318 (scan code 138) on this phone. */
    static boolean isFavoritesKey(KeyEvent e) {
        return e.getKeyCode() == 318 || e.getScanCode() == 138;
    }

    void openNotifications() {
        startActivity(new Intent(this, NotificationsActivity.class));
    }

    void openApps() {
        startActivity(new Intent(this, AppsActivity.class));
    }

    private void updateHeader() {
        Date now = new Date();
        time.setText(DateFormat.format(DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm a", now));
        date.setText(DateFormat.format("EEE, MMM d", now));
        carrier.setText(carrierName());
    }

    private String carrierName() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null || tm.getSimState() == TelephonyManager.SIM_STATE_ABSENT) {
            return "No SIM card";
        }
        String name = tm.getNetworkOperatorName();
        if (TextUtils.isEmpty(name)) name = tm.getSimOperatorName();
        return TextUtils.isEmpty(name) ? "No service" : name;
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = this;
        updateHeader();
        if (!receiverRegistered) {
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_TIME_TICK);
            f.addAction(Intent.ACTION_TIME_CHANGED);
            f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            f.addAction(Intent.ACTION_DATE_CHANGED);
            f.addAction("android.intent.action.SIM_STATE_CHANGED");
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(tickReceiver, f, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(tickReceiver, f);
            }
            receiverRegistered = true;
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (visible == this) visible = null;
        if (receiverRegistered) {
            try { unregisterReceiver(tickReceiver); } catch (RuntimeException ignored) { }
            receiverRegistered = false;
        }
        super.onPause();
    }

    /** HOME pressed while already home: jump back to the first entry. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        updateHeader();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateHeader();
    }

    // ---- keys -------------------------------------------------------------------------

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.KEYCODE_SOFT_LEFT || isFavoritesKey(e)) {
            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
                openNotifications();
            }
            return true;
        }
        if (Prefs.isQuickMenuKey(this, e)) {
            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) openApps();
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                if (e.getRepeatCount() == 0) {
                    centerLong = false;
                } else if (!centerLong) {
                    centerLong = true;
                    showQuickMenu();
                }
            } else if (e.getAction() == KeyEvent.ACTION_UP && !centerLong) {
                openApps();
            }
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                if (e.getRepeatCount() == 0) {
                    dpadLong = false;
                } else if (!dpadLong) {
                    dpadLong = true;
                    startActivity(new Intent(this, PickAppsActivity.class)
                            .putExtra(PickAppsActivity.EXTRA_DPAD, code)
                            .putExtra(PickAppsActivity.EXTRA_TITLE, "Set shortcut"));
                }
            } else if (e.getAction() == KeyEvent.ACTION_UP && !dpadLong) {
                String id = Prefs.shortcut(this, code);
                AppItem it = id == null ? null : Entries.resolve(this, id);
                if (it != null) Entries.launch(this, it);
            }
            return true;
        }
        boolean digit = code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9;
        boolean star = code == KeyEvent.KEYCODE_STAR;
        boolean pound = code == KeyEvent.KEYCODE_POUND;
        if (digit || star || pound) {
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                if (pound) {
                    if (e.getRepeatCount() > 0) {
                        if (!poundLong) {
                            poundLong = true;
                            toggleRinger();
                        }
                    } else {
                        poundLong = false;
                    }
                } else if (e.getRepeatCount() == 0) {
                    dial(star ? "*" : String.valueOf(code - KeyEvent.KEYCODE_0));
                }
            } else if (e.getAction() == KeyEvent.ACTION_UP && pound) {
                if (!poundLong) dial("#");
                poundLong = false;
            }
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    /** BACK does nothing on the home screen. */
    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) return true;
        return super.onKeyDown(code, e);
    }

    @Override
    public void onBackPressed() {
        // Stay on the home screen.
    }

    // ---- quick menu -------------------------------------------------------------------

    private static HomeActivity visible;
    private AlertDialog quickDialog;

    /** The home screen while it is on screen, else null (used by the accessibility key filter). */
    static HomeActivity visibleHome() {
        return visible;
    }

    /** Open the quick menu, or close it if it is already open. */
    void toggleQuickMenu() {
        if (quickDialog != null && quickDialog.isShowing()) {
            quickDialog.dismiss();
            quickDialog = null;
        } else {
            showQuickMenu();
        }
    }

    private String[] quickLabels() {
        android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        android.bluetooth.BluetoothAdapter bt = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int mode = am == null ? AudioManager.RINGER_MODE_NORMAL : am.getRingerMode();
        String sound = mode == AudioManager.RINGER_MODE_NORMAL ? "Sound"
                : mode == AudioManager.RINGER_MODE_VIBRATE ? "Vibrate" : "Silent";
        return new String[] {
                "Wi-Fi: " + (wm != null && wm.isWifiEnabled() ? "On" : "Off"),
                "Bluetooth: " + (bt != null && bt.isEnabled() ? "On" : "Off"),
                "Hotspot",
                "Flashlight",
                "Sound: " + sound,
                "Wallpapers",
                "Wallpaper from photos"};
    }

    private void showQuickMenu() {
        quickDialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Quick menu")
                .setItems(quickLabels(), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        runQuickToggle(which);
                    }
                })
                .show();
    }

    // Android 10+ stops apps switching radios directly, so those open the system screen.
    private void runQuickToggle(int which) {
        try {
            switch (which) {
                case 0: startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); break;
                case 1: startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); break;
                case 2:
                    try {
                        startActivity(new Intent().setClassName("com.android.settings",
                                "com.android.settings.TetherSettings"));
                    } catch (RuntimeException e) {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                    break;
                case 3: startActivity(new Intent(this, TorchActivity.class)); break;
                case 4: cycleSound(); break;
                case 5:
                    showWallpaperList();
                    break;
                default:
                    startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), 77);
            }
        } catch (RuntimeException e) {
            android.widget.Toast.makeText(this, "Not available.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != 77 || res != RESULT_OK || data == null || data.getData() == null) return;
        showWallpaperPreview(-1, data.getData());
    }

    private void showWallpaperList() {
        showWallpaperPreview(0, null);
    }

    private int wpIndex;

    private android.graphics.Bitmap previewBitmap(int idx, android.net.Uri photo) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        try {
            if (photo == null) return Wallpapers.render(idx, dm.widthPixels / 3, dm.heightPixels / 3);
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inSampleSize = 4;
            java.io.InputStream in = getContentResolver().openInputStream(photo);
            android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeStream(in, null, o);
            in.close();
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    // photo == null browses the built-in set with left/right; otherwise previews that photo.
    private void showWallpaperPreview(int start, final android.net.Uri photo) {
        wpIndex = start;
        final android.widget.ImageView iv = new android.widget.ImageView(this);
        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        iv.setMinimumHeight(UI.dp(this, 300));
        iv.setImageBitmap(previewBitmap(wpIndex, photo));
        final AlertDialog dlg = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(photo == null ? Wallpapers.NAMES[wpIndex] : "Preview")
                .setView(iv)
                .setPositiveButton("Set (home + lock)", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        try {
                            android.app.WallpaperManager wm = android.app.WallpaperManager.getInstance(HomeActivity.this);
                            if (photo == null) {
                                android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                                wm.setBitmap(Wallpapers.render(wpIndex, dm.widthPixels, dm.heightPixels), null, true,
                                        android.app.WallpaperManager.FLAG_SYSTEM | android.app.WallpaperManager.FLAG_LOCK);
                            } else {
                                java.io.InputStream in = getContentResolver().openInputStream(photo);
                                wm.setStream(in, null, true, android.app.WallpaperManager.FLAG_SYSTEM | android.app.WallpaperManager.FLAG_LOCK);
                                in.close();
                            }
                        } catch (Exception e) {
                            android.widget.Toast.makeText(HomeActivity.this, "Could not set wallpaper.",
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        if (photo == null) {
            dlg.setButton(AlertDialog.BUTTON_NEUTRAL, "Next", (DialogInterface.OnClickListener) null);
        }
        dlg.show();
        if (photo == null) {
            final int n = Wallpapers.NAMES.length;
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    wpIndex = (wpIndex + 1) % n;
                    iv.setImageBitmap(previewBitmap(wpIndex, null));
                    dlg.setTitle(Wallpapers.NAMES[wpIndex]);
                }
            });
        }
    }

    private void cycleSound() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        int next = am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL
                ? AudioManager.RINGER_MODE_VIBRATE
                : am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE
                        ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL;
        try {
            am.setRingerMode(next);
        } catch (SecurityException e) {
            android.widget.Toast.makeText(this, "Blocked by Do Not Disturb.",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void dial(String number) {
        Intent i = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (RuntimeException e) {
            android.widget.Toast.makeText(this, "App isn't installed.",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleRinger() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        try {
            if (am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            } else {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            }
        } catch (SecurityException ignored) {
            // Some builds restrict ringer changes while Do Not Disturb is active.
        }
    }
}
