package com.JMTDITech.JLauncher;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The quick menu drawn as a full-screen, opaque accessibility overlay. It appears above every app,
 * so when the phone opens its own app for the menu key (Contacts) that app is hidden behind it.
 * The overlay cannot take focus, so keys arrive through the service's key filter (see onKey).
 */
final class OverlayMenu {
    private static final long AUTO_DISMISS_MS = 20000;
    /** Ignore other apps' windows for this long after showing: that is the phone's own app launching. */
    static final long GRACE_MS = 1500;

    private final AccessibilityService svc;
    private final WindowManager wm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoDismiss = new Runnable() {
        @Override public void run() { dismiss(); }
    };

    private LinearLayout root;
    private LinearLayout[] rows;
    private int selected;
    private long shownAt;

    OverlayMenu(AccessibilityService svc) {
        this.svc = svc;
        this.wm = (WindowManager) svc.getSystemService(AccessibilityService.WINDOW_SERVICE);
    }

    boolean isShowing() {
        return root != null;
    }

    long shownAt() {
        return shownAt;
    }

    void show() {
        if (root != null) return;
        String[] labels = HomeActivity.QUICK_LABELS;

        LinearLayout col = new LinearLayout(svc);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackground(UI.screenBackground());
        col.setPadding(0, statusBarHeight(), 0, 0);
        col.addView(UI.title(svc, "Quick menu"));

        rows = new LinearLayout[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(svc);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(UI.listRowBackground());
            row.setPadding(UI.dp(svc, 16), 0, UI.dp(svc, 8), 0);

            TextView tv = new TextView(svc);
            tv.setText(labels[i]);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tv.setTextColor(UI.listRowText());
            tv.setSingleLine(true);
            tv.setDuplicateParentStateEnabled(true);
            row.addView(tv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    selected = index;
                    updateSelection();
                    activate();
                }
            });
            rows[i] = row;
            col.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(svc, 52)));
        }
        selected = 0;
        updateSelection();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE);
        try {
            wm.addView(col, lp);
        } catch (RuntimeException e) {
            android.util.Log.e("JLauncher", "Could not show overlay menu", e);
            rows = null;
            return;
        }
        root = col;
        shownAt = SystemClock.uptimeMillis();
        handler.removeCallbacks(autoDismiss);
        handler.postDelayed(autoDismiss, AUTO_DISMISS_MS);
    }

    void dismiss() {
        handler.removeCallbacks(autoDismiss);
        if (root == null) return;
        try {
            wm.removeView(root);
        } catch (RuntimeException ignored) {
            // already gone
        }
        root = null;
        rows = null;
    }

    /** Handle a key while the overlay is showing. Returns true if the key was consumed. */
    boolean onKey(KeyEvent e) {
        if (root == null) return false;
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (down) move(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (down) move(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (down && e.getRepeatCount() == 0) activate();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (down) dismiss();
                return true;
            default:
                return false;
        }
    }

    private void move(int delta) {
        if (rows == null) return;
        selected = (selected + delta + rows.length) % rows.length;
        updateSelection();
        handler.removeCallbacks(autoDismiss);
        handler.postDelayed(autoDismiss, AUTO_DISMISS_MS);
    }

    private void updateSelection() {
        if (rows == null) return;
        for (int i = 0; i < rows.length; i++) rows[i].setSelected(i == selected);
    }

    private void activate() {
        final int which = selected;
        dismiss();
        handler.postDelayed(new Runnable() {
            @Override public void run() { perform(which); }
        }, 120);
    }

    private void perform(final int which) {
        HomeActivity home = HomeActivity.visibleHome();
        if (home != null) {
            home.runQuickAction(which);
            return;
        }
        // Home isn't showing yet (the phone's own app may still be on top): go home, retry once.
        svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                HomeActivity h = HomeActivity.visibleHome();
                if (h != null) h.runQuickAction(which);
            }
        }, 700);
    }

    private int statusBarHeight() {
        int id = svc.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? svc.getResources().getDimensionPixelSize(id) : 0;
    }
}
