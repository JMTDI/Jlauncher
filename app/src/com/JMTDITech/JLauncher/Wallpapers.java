package com.JMTDITech.JLauncher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import java.util.Random;

/** Ten built-in wallpapers drawn in code: a vertical gradient plus soft shapes. */
final class Wallpapers {
    static final String[] NAMES = {"Midnight", "Sunset", "Ocean", "Forest", "Aurora",
            "Ember", "Lavender", "Mint", "Rose Gold", "Slate"};
    private static final int[][] COLORS = {
            {0xFF0B1026, 0xFF2B3A8C}, {0xFFFF5E3A, 0xFF5B1E7A}, {0xFF021B3A, 0xFF1CA3C4},
            {0xFF06281A, 0xFF4CAF50}, {0xFF0A1F3F, 0xFF22E6A8}, {0xFF2B0A0A, 0xFFFF8F1F},
            {0xFF2A1B4D, 0xFFB79CFF}, {0xFF0F4C4C, 0xFFA8F0D0}, {0xFF3B1F2B, 0xFFF4B6A6},
            {0xFF111418, 0xFF5A6675}};

    static Bitmap render(int index, int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] col = COLORS[index];
        p.setShader(new LinearGradient(0, 0, w * 0.4f, h, col[0], col[1], Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        Random r = new Random(index * 31L + 7);
        for (int i = 0; i < 7; i++) {
            p.setColor(i % 2 == 0 ? 0xFFFFFFFF : col[1]);
            p.setAlpha(18 + r.nextInt(26));
            c.drawCircle(r.nextFloat() * w, r.nextFloat() * h, w * (0.15f + r.nextFloat() * 0.45f), p);
        }
        return bmp;
    }
}
