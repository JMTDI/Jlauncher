package com.JMTDITech.JLauncher;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** One adapter for every list: numbered rows, grid cells, text-only rows, and the app picker. */
final class AppAdapter extends BaseAdapter {
    static final int STYLE_LIST = 0;   // white rows: number, icon, name
    static final int STYLE_GRID = 1;   // icon plate + name
    static final int STYLE_TEXT = 2;   // transparent rows, name only
    static final int STYLE_PICK = 3;   // like LIST, number shows pick order

    private final Context ctx;
    private final List<AppItem> items;
    private final int style;
    private List<String> picked = new ArrayList<String>();

    AppAdapter(Context ctx, List<AppItem> items, int style) {
        this.ctx = ctx;
        this.items = items;
        this.style = style;
    }

    void setPicked(List<String> picked) {
        this.picked = picked;
    }

    @Override public int getCount() { return items.size(); }
    @Override public Object getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    private static final class Holder {
        ImageView icon;
        TextView name;
        TextView index;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView != null ? convertView : create();
        Holder h = (Holder) v.getTag();
        AppItem it = items.get(position);
        h.name.setText(it.label);
        if (h.icon != null) h.icon.setImageDrawable(it.icon);
        if (h.index != null) {
            if (style == STYLE_PICK) {
                int i = picked.indexOf(it.id);
                h.index.setText(i >= 0 ? String.valueOf(i + 1) : "");
            } else {
                h.index.setText(String.valueOf(position + 1));
            }
        }
        return v;
    }

    private View create() {
        switch (style) {
            case STYLE_GRID: return createGrid();
            case STYLE_TEXT: return createText();
            default: return createList();
        }
    }

    private View createList() {
        Holder h = new Holder();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(UI.listRowBackground());
        row.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(ctx, 52)));

        h.index = new TextView(ctx);
        h.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        h.index.setTextColor(UI.listRowText());
        h.index.setGravity(Gravity.CENTER);
        h.index.setSingleLine(true);
        h.index.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        h.index.setDuplicateParentStateEnabled(true);
        row.addView(h.index, new LinearLayout.LayoutParams(UI.dp(ctx, 40),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        h.icon = new ImageView(ctx);
        h.icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(h.icon, new LinearLayout.LayoutParams(UI.dp(ctx, 30), UI.dp(ctx, 30)));

        h.name = new TextView(ctx);
        h.name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        h.name.setTextColor(UI.listRowText());
        h.name.setSingleLine(true);
        h.name.setEllipsize(TextUtils.TruncateAt.END);
        h.name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        h.name.setDuplicateParentStateEnabled(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = UI.dp(ctx, 10);
        lp.rightMargin = UI.dp(ctx, 8);
        row.addView(h.name, lp);

        row.setTag(h);
        return row;
    }

    private View createText() {
        Holder h = new Holder();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(UI.moreRowBackground());
        row.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(ctx, 48)));

        h.index = new TextView(ctx);
        h.index.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        h.index.setTextColor(UI.moreRowText());
        h.index.setGravity(Gravity.CENTER);
        h.index.setSingleLine(true);
        h.index.setDuplicateParentStateEnabled(true);
        row.addView(h.index, new LinearLayout.LayoutParams(UI.dp(ctx, 40),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        h.name = new TextView(ctx);
        h.name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        h.name.setTextColor(UI.moreRowText());
        h.name.setSingleLine(true);
        h.name.setEllipsize(TextUtils.TruncateAt.END);
        h.name.setDuplicateParentStateEnabled(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = UI.dp(ctx, 8);
        row.addView(h.name, lp);

        row.setTag(h);
        return row;
    }

    private View createGrid() {
        Holder h = new Holder();
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = UI.dp(ctx, 10);
        cell.setPadding(0, pad, 0, pad);
        cell.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout plate = new FrameLayout(ctx);
        plate.setBackground(UI.gridIconBackground(ctx));
        plate.setDuplicateParentStateEnabled(true);
        h.icon = new ImageView(ctx);
        h.icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(UI.dp(ctx, 56), UI.dp(ctx, 56));
        ilp.gravity = Gravity.CENTER;
        plate.addView(h.icon, ilp);
        cell.addView(plate, new LinearLayout.LayoutParams(UI.dp(ctx, 76), UI.dp(ctx, 76)));

        h.name = new TextView(ctx);
        h.name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        h.name.setTextColor(UI.WHITE);
        h.name.setGravity(Gravity.CENTER_HORIZONTAL);
        h.name.setMaxLines(2);
        h.name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = UI.dp(ctx, 6);
        cell.addView(h.name, nlp);

        cell.setTag(h);
        return cell;
    }
}
