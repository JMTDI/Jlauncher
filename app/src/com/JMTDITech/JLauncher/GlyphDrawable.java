package com.JMTDITech.JLauncher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Simple built-in icons for JLauncher's own entries (Tools, More, Flashlight). */
final class GlyphDrawable extends Drawable {
    private final String kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    GlyphDrawable(String kind) {
        this.kind = kind;
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float w = b.width();
        float h = b.height();
        if (w <= 0 || h <= 0) return;
        float l = b.left;
        float t = b.top;
        float cx = l + w / 2f;
        float cy = t + h / 2f;

        if ("gridicon".equals(kind) || "listicon".equals(kind)) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFD0D3DA);
            if ("gridicon".equals(kind)) {
                float s = w * 0.38f;
                float g = w * 0.08f;
                float x0 = cx - (2 * s + g) / 2f;
                float y0 = cy - (2 * s + g) / 2f;
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        float x = x0 + i * (s + g);
                        float y = y0 + j * (s + g);
                        c.drawRoundRect(new RectF(x, y, x + s, y + s), s * 0.3f, s * 0.3f, p);
                    }
                }
            } else {
                float rh = w * 0.2f;
                for (int k = 0; k < 3; k++) {
                    float y = t + w * 0.08f + k * w * 0.32f;
                    c.drawRoundRect(new RectF(l + w * 0.05f, y, l + w * 0.25f, y + rh), rh * 0.3f, rh * 0.3f, p);
                    c.drawRoundRect(new RectF(l + w * 0.33f, y, l + w * 0.95f, y + rh), rh * 0.3f, rh * 0.3f, p);
                }
            }
            return;
        }

        int bg;
        if ("tools".equals(kind)) bg = 0xFF43A047;
        else if ("flashlight".equals(kind)) bg = 0xFFF9A825;
        else bg = 0xFF546E7A;

        p.setStyle(Paint.Style.FILL);
        p.setColor(bg);
        c.drawRoundRect(new RectF(l, t, l + w, t + h), w * 0.22f, w * 0.22f, p);
        p.setColor(0xFFFFFFFF);

        if ("tools".equals(kind)) {
            float s = w * 0.2f;
            float g = w * 0.08f;
            float x0 = cx - (2 * s + g) / 2f;
            float y0 = cy - (2 * s + g) / 2f;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    float x = x0 + i * (s + g);
                    float y = y0 + j * (s + g);
                    c.drawRoundRect(new RectF(x, y, x + s, y + s), s * 0.2f, s * 0.2f, p);
                }
            }
        } else if ("flashlight".equals(kind)) {
            c.drawCircle(cx, cy, w * 0.15f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(w * 0.05f);
            p.setStrokeCap(Paint.Cap.ROUND);
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * 2 * k / 8;
                float cs = (float) Math.cos(a);
                float sn = (float) Math.sin(a);
                c.drawLine(cx + cs * w * 0.24f, cy + sn * w * 0.24f,
                        cx + cs * w * 0.34f, cy + sn * w * 0.34f, p);
            }
        } else {
            float r = w * 0.07f;
            for (int k = -1; k <= 1; k++) {
                c.drawCircle(cx + k * w * 0.24f, cy, r, p);
            }
        }
    }

    @Override public void setAlpha(int alpha) { p.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter cf) { p.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
