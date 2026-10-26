package com.JMTDITech.JLauncher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared look: navy background, white rows, blue focus highlight (matches the original launcher). */
final class UI {
    static final int NAVY_TOP = 0xFF1E2A59;
    static final int NAVY_BOTTOM = 0xFF172050;
    static final int BLUE = 0xFF3478F6;
    static final int WHITE = 0xFFFFFFFF;
    static final int BLACK = 0xFF000000;
    static final int DIVIDER = 0xFFE5E5E5;
    static final int GRAY_TEXT = 0x99FFFFFF;

    private static final int[] SELECTED = {android.R.attr.state_selected};
    private static final int[] FOCUSED = {android.R.attr.state_focused};
    private static final int[] PRESSED = {android.R.attr.state_pressed};
    private static final int[] ANY = {};

    private UI() {}

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static Drawable screenBackground() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {NAVY_TOP, NAVY_BOTTOM});
    }

    /** White row with a thin divider; solid blue when selected, focused or pressed. */
    static Drawable listRowBackground() {
        StateListDrawable s = new StateListDrawable();
        s.addState(SELECTED, new ColorDrawable(BLUE));
        s.addState(FOCUSED, new ColorDrawable(BLUE));
        s.addState(PRESSED, new ColorDrawable(BLUE));
        LayerDrawable normal = new LayerDrawable(
                new Drawable[] {new ColorDrawable(DIVIDER), new ColorDrawable(WHITE)});
        normal.setLayerInset(1, 0, 0, 0, 1);
        s.addState(ANY, normal);
        return s;
    }

    static ColorStateList listRowText() {
        return new ColorStateList(
                new int[][] {SELECTED, FOCUSED, PRESSED, ANY},
                new int[] {WHITE, WHITE, WHITE, BLACK});
    }

    /** Transparent row; white when selected (used by the More folder). */
    static Drawable moreRowBackground() {
        StateListDrawable s = new StateListDrawable();
        s.addState(SELECTED, new ColorDrawable(WHITE));
        s.addState(FOCUSED, new ColorDrawable(WHITE));
        s.addState(PRESSED, new ColorDrawable(WHITE));
        s.addState(ANY, new ColorDrawable(0x00000000));
        return s;
    }

    static ColorStateList moreRowText() {
        return new ColorStateList(
                new int[][] {SELECTED, FOCUSED, PRESSED, ANY},
                new int[] {BLACK, BLACK, BLACK, WHITE});
    }

    /** Rounded white plate behind a grid icon when it is selected. */
    static Drawable gridIconBackground(Context c) {
        StateListDrawable s = new StateListDrawable();
        GradientDrawable on = new GradientDrawable();
        on.setColor(WHITE);
        on.setCornerRadius(dp(c, 16));
        s.addState(SELECTED, on);
        s.addState(FOCUSED, on);
        s.addState(PRESSED, on);
        s.addState(ANY, new ColorDrawable(0x00000000));
        return s;
    }

    /** Bold white screen title, as in the original action bar. */
    static TextView title(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(WHITE);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 44)));
        return t;
    }
}
