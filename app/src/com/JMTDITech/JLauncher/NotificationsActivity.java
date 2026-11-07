package com.JMTDITech.JLauncher;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** JLauncher's own notification list. SOFT_LEFT or BACK closes it; # clears all, * clears the highlighted one. */
public class NotificationsActivity extends Activity {
    private final List<StatusBarNotification> items = new ArrayList<StatusBarNotification>();
    private BaseAdapter adapter;
    private TextView empty;
    private ListView list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(UI.screenBackground());
        getWindow().setStatusBarColor(0xFF0B0D10);
        getWindow().setNavigationBarColor(UI.NAVY_BOTTOM);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(UI.title(this, "Notifications"));

        FrameLayout area = new FrameLayout(this);
        list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(new ColorDrawable(0));
        list.setVerticalScrollBarEnabled(false);
        adapter = new Adapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                open(items.get(pos));
            }
        });
        area.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        empty = new TextView(this);
        empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        empty.setTextColor(UI.GRAY_TEXT);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(UI.dp(this, 24), 0, UI.dp(this, 24), 0);
        empty.setFocusable(true);
        empty.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { allowAccess(); }
        });
        area.addView(empty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        list.setEmptyView(empty);

        root.addView(area, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(UI.softkeyBar(this, "* this", "# all"));
        setContentView(root);
        list.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotifListener.setOnChange(new Runnable() {
            @Override public void run() {
                runOnUiThread(new Runnable() {
                    @Override public void run() { reload(); }
                });
            }
        });
        reload();
    }

    @Override
    protected void onPause() {
        NotifListener.setOnChange(null);
        super.onPause();
    }

    private void reload() {
        items.clear();
        for (StatusBarNotification n : NotifListener.active()) {
            if ((n.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) == 0) items.add(n);
        }
        java.util.Collections.sort(items, new java.util.Comparator<StatusBarNotification>() {
            @Override public int compare(StatusBarNotification a, StatusBarNotification b) {
                return Long.compare(b.getPostTime(), a.getPostTime());
            }
        });
        empty.setText(NotifListener.isConnected()
                ? "No notifications"
                : "Notification access is off.\nPress OK to allow JLauncher.");
        adapter.notifyDataSetChanged();
    }

    private void allowAccess() {
        if (NotifListener.isConnected()) return;
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (RuntimeException ignored) {
            // no such settings screen on this phone
        }
    }

    private void open(StatusBarNotification n) {
        PendingIntent pi = n.getNotification().contentIntent;
        if (pi != null) {
            try { pi.send(); } catch (PendingIntent.CanceledException ignored) { }
        }
        if ((n.getNotification().flags & Notification.FLAG_AUTO_CANCEL) != 0) {
            NotifListener.clear(n.getKey());
        }
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
            finish();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_POUND || keyCode == KeyEvent.KEYCODE_MENU
                || keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            NotifListener.clearAll();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_STAR) {
            int pos = list.getSelectedItemPosition();
            if (pos >= 0 && pos < items.size()) NotifListener.clear(items.get(pos).getKey());
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int i) { return items.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            Context c = NotificationsActivity.this;
            LinearLayout row = (LinearLayout) convert;
            if (row == null) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(UI.listRowBackground());
                row.setPadding(UI.dp(c, 12), 0, UI.dp(c, 8), 0);
                row.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 58)));
                ImageView icon = new ImageView(c);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                row.addView(icon, new LinearLayout.LayoutParams(UI.dp(c, 28), UI.dp(c, 28)));
                LinearLayout col = new LinearLayout(c);
                col.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                cp.leftMargin = UI.dp(c, 10);
                row.addView(col, cp);
                col.addView(line(c, 15, UI.WHITE));
                col.addView(line(c, 12, UI.GRAY_TEXT));
            }
            StatusBarNotification n = items.get(pos);
            Notification no = n.getNotification();
            CharSequence title = no.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text = no.extras.getCharSequence(Notification.EXTRA_TEXT);
            ((ImageView) row.getChildAt(0)).setImageDrawable(appIcon(n.getPackageName()));
            LinearLayout col = (LinearLayout) row.getChildAt(1);
            ((TextView) col.getChildAt(0)).setText(title == null ? appLabel(n.getPackageName()) : title);
            ((TextView) col.getChildAt(1)).setText(text == null ? "" : text);
            return row;
        }

        private TextView line(Context c, int sp, int color) {
            TextView t = new TextView(c);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
            t.setTextColor(color);
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            return t;
        }

        private Drawable appIcon(String pkg) {
            try {
                return getPackageManager().getApplicationIcon(pkg);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }

        private String appLabel(String pkg) {
            try {
                return getPackageManager().getApplicationLabel(
                        getPackageManager().getApplicationInfo(pkg, 0)).toString();
            } catch (PackageManager.NameNotFoundException e) {
                return pkg;
            }
        }
    }
}
