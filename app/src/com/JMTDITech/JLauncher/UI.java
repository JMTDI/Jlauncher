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

/** Shared look: near-black background, light text, grey focus highlight, bottom softkey bar. */
final class UI {
    static final int NAVY_TOP = 0xFF12151A;
    static final int NAVY_BOTTOM = 0xFF0B0D10;
    static final int BLUE = 0xFF37393E;
    static final int WHITE = 0xFFE1E2E8;
    static final int BLACK = 0xFFE1E2E8;
    static final int DIVIDER = 0xFF43474E;
    static final int GRAY_TEXT = 0xFFB0B4BC;
    static final int SURFACE = 0xFF272A2F;

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
        return new ColorDrawable(NAVY_TOP);
    }

    /** Transparent row; grey highlight when selected, focused or pressed. */
    static Drawable listRowBackground() {
        StateListDrawable s = new StateListDrawable();
        s.addState(SELECTED, new ColorDrawable(BLUE));
        s.addState(FOCUSED, new ColorDrawable(BLUE));
        s.addState(PRESSED, new ColorDrawable(BLUE));
        s.addState(ANY, new ColorDrawable(0x00000000));
        return s;
    }

    static ColorStateList listRowText() {
        return ColorStateList.valueOf(WHITE);
    }

    static Drawable moreRowBackground() {
        return listRowBackground();
    }

    static ColorStateList moreRowText() {
        return listRowText();
    }

    /** Rounded grey plate behind a grid icon when it is selected. */
    static Drawable gridIconBackground(Context c) {
        StateListDrawable s = new StateListDrawable();
        GradientDrawable on = new GradientDrawable();
        on.setColor(BLUE);
        on.setCornerRadius(dp(c, 16));
        s.addState(SELECTED, on);
        s.addState(FOCUSED, on);
        s.addState(PRESSED, on);
        s.addState(ANY, new ColorDrawable(0x00000000));
        return s;
    }

    /** Small left-aligned section header with a thin rule below. */
    static TextView title(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setLetterSpacing(0.08f);
        t.setAllCaps(true);
        t.setTextColor(GRAY_TEXT);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        t.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 32)));
        return t;
    }

    /**
     * Bottom bar with an optional left text and a glyph (tagged "glyph", text tagged "text").
     * The glyph sits at the bar's centre when {@code center} is set, otherwise at the left.
     * {@code overWallpaper} makes the fill translucent so the wallpaper shows through.
     */
    static LinearLayout glyphBar(Context c, String text, String glyph, boolean center,
                                 boolean overWallpaper) {
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        LayerDrawable bg = new LayerDrawable(new Drawable[] {
                new ColorDrawable(overWallpaper ? 0x44FFFFFF : DIVIDER),
                new ColorDrawable(overWallpaper ? 0xCC0B0D10 : NAVY_BOTTOM)});
        bg.setLayerInset(1, 0, dp(c, 1), 0, 0);
        bar.setBackground(bg);
        bar.setPadding(dp(c, 8), dp(c, 1), dp(c, 8), 0);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 38)));

        if (text != null) {
            TextView t = new TextView(c);
            t.setTag("text");
            t.setText(text);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            t.setTextColor(WHITE);
            t.setSingleLine(true);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(dp(c, 4), 0, 0, 0);
            bar.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        android.widget.ImageView g = new android.widget.ImageView(c);
        g.setTag("glyph");
        g.setImageDrawable(new GlyphDrawable(glyph));
        g.setPadding(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        bar.addView(g, new LinearLayout.LayoutParams(dp(c, 30), ViewGroup.LayoutParams.MATCH_PARENT));
        if (center || text == null) {
            bar.addView(new android.view.View(c),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        return bar;
    }

    /** Bottom bar: 1dp rule, darker fill, left and right text softkeys (null hides one). */
    static LinearLayout softkeyBar(Context c, String left, String right) {
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(new LayerDrawable(new Drawable[] {
                new ColorDrawable(DIVIDER), new ColorDrawable(NAVY_BOTTOM)}));
        ((LayerDrawable) bar.getBackground()).setLayerInset(1, 0, dp(c, 1), 0, 0);
        bar.setPadding(dp(c, 12), dp(c, 1), dp(c, 12), 0);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 38)));
        bar.addView(softkey(c, left, Gravity.START));
        bar.addView(softkey(c, right, Gravity.END));
        return bar;
    }

    private static TextView softkey(Context c, String text, int gravity) {
        TextView t = new TextView(c);
        t.setText(text == null ? "" : text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTextColor(WHITE);
        t.setSingleLine(true);
        t.setGravity(gravity | Gravity.CENTER_VERTICAL);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return t;
    }
}
