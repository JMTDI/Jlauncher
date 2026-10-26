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
    private TextView title;
    private TextView date;
    private TextView carrier;
    private ListView listView;
    private GridView gridView;
    private AppAdapter listAdapter;
    private AppAdapter gridAdapter;
    private final List<AppItem> items = new ArrayList<AppItem>();
    private boolean poundLong;
    private boolean receiverRegistered;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateHeader(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(UI.screenBackground());
        getWindow().setStatusBarColor(UI.NAVY_TOP);
        getWindow().setNavigationBarColor(UI.NAVY_BOTTOM);
        setContentView(buildContent());
        listAdapter = new AppAdapter(this, items, AppAdapter.STYLE_LIST);
        gridAdapter = new AppAdapter(this, items, AppAdapter.STYLE_GRID);
        refresh(true);
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        title = UI.title(this, "Home Menu");
        root.addView(title);

        date = new TextView(this);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        date.setTextColor(UI.GRAY_TEXT);
        date.setGravity(Gravity.CENTER);
        date.setSingleLine(true);
        date.setShadowLayer(2f, 1f, 1f, 0x80000000);
        root.addView(date, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        carrier = new TextView(this);
        carrier.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        carrier.setTextColor(UI.GRAY_TEXT);
        carrier.setGravity(Gravity.CENTER);
        carrier.setSingleLine(true);
        carrier.setShadowLayer(2f, 1f, 1f, 0x80000000);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = UI.dp(this, 2);
        clp.bottomMargin = UI.dp(this, 8);
        root.addView(carrier, clp);

        FrameLayout area = new FrameLayout(this);

        listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setSelector(new ColorDrawable(0));
        listView.setVerticalScrollBarEnabled(false);
        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        area.addView(listView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        gridView = new GridView(this);
        gridView.setNumColumns(2);
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        gridView.setVerticalSpacing(UI.dp(this, 4));
        gridView.setSelector(new ColorDrawable(0));
        gridView.setVerticalScrollBarEnabled(false);
        gridView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        area.addView(gridView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        AdapterView.OnItemClickListener click = new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (pos >= 0 && pos < items.size()) Entries.launch(HomeActivity.this, items.get(pos));
            }
        };
        listView.setOnItemClickListener(click);
        gridView.setOnItemClickListener(click);

        root.addView(area, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    /** Reload the active list; keep the highlighted row unless resetSelection is set. */
    private void refresh(boolean resetSelection) {
        boolean grid = Prefs.layout(this) == Prefs.LAYOUT_GRID;
        AbsListView active = grid ? gridView : listView;
        int keep = resetSelection ? 0 : Math.max(0, active.getSelectedItemPosition());

        items.clear();
        items.addAll(Entries.resolveAll(this,
                Prefs.getList(this, grid ? Prefs.KEY_HOME_GRID : Prefs.KEY_HOME_LIST)));

        title.setVisibility(grid ? View.GONE : View.VISIBLE);
        date.setVisibility(grid ? View.GONE : View.VISIBLE);
        listView.setVisibility(grid ? View.GONE : View.VISIBLE);
        gridView.setVisibility(grid ? View.VISIBLE : View.GONE);
        // Only the visible view holds the adapter, so icons are never shared between two views.
        listView.setAdapter(grid ? null : listAdapter);
        gridView.setAdapter(grid ? gridAdapter : null);

        if (!items.isEmpty()) {
            active.setSelection(Math.min(keep, items.size() - 1));
        }
        active.requestFocus();
        updateHeader();
    }

    private void updateHeader() {
        date.setText(DateFormat.format("EEEE, MMM dd", new Date()));
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
        refresh(false);
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
        refresh(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refresh(false);
    }

    // ---- keys -------------------------------------------------------------------------

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (Prefs.isQuickMenuKey(this, e)) {
            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) toggleQuickMenu();
            return true;
        }
        int code = e.getKeyCode();
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

    static final String[] QUICK_LABELS = {"Notifications", "Quick settings", "JLauncher settings"};

    private void showQuickMenu() {
        quickDialog = new AlertDialog.Builder(this)
                .setTitle("Quick menu")
                .setItems(QUICK_LABELS, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        runQuickAction(which);
                    }
                })
                .show();
    }

    /** Run one quick-menu entry (also called by the overlay menu). */
    void runQuickAction(int which) {
        if (which == 0) openShade(false);
        else if (which == 1) openShade(true);
        else startActivity(new Intent(this, SettingsActivity.class));
    }

    /**
     * 1) the phone's own SystemUI screen (what the original launcher used);
     * 2) the standard shade through the accessibility service, checking that something opened;
     * 3) otherwise explain what is missing instead of failing silently.
     */
    private void openShade(final boolean quick) {
        // Let the menu dialog finish closing first.
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                final String why = SystemShade.tryOpen(HomeActivity.this, quick);
                if (why == null) return;
                if (!NotificationService.isConnected()) {
                    promptAccessibility(why);
                    return;
                }
                final long before = NotificationService.lastWindowEventTime();
                final boolean accepted = NotificationService.performShadeAction(quick);
                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        if (!accepted || NotificationService.lastWindowEventTime() == before) {
                            showFailure(quick, why);
                        }
                    }
                }, 900);
            }
        }, 150);
    }

    private void promptAccessibility(String why) {
        new AlertDialog.Builder(this)
                .setTitle("Couldn't open it directly")
                .setMessage("The phone's own screen for this didn't open (" + why + ").\n\n"
                        + "Turn on the JLauncher accessibility service and JLauncher can try the "
                        + "standard shade instead. It also helps the quick-menu key work without "
                        + "also triggering the phone's own action for that key.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                })
                .show();
    }

    private void showFailure(boolean quick, String why) {
        new AlertDialog.Builder(this)
                .setTitle("Nothing opened")
                .setMessage("The phone's own " + (quick ? "quick settings" : "notifications")
                        + " screen didn't open (" + why + "), and the standard shade didn't "
                        + "respond either. This phone may only allow its own apps to open it.")
                .setPositiveButton("OK", null)
                .show();
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
