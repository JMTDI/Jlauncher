package com.JMTDITech.JLauncher;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Apps screen: numbered list (Tools expanded inline) or 3-column grid. MENU toggles. */
public class AppsActivity extends Activity {
    private static final class Row {
        String header;   // non-null for a section header
        String number;
        AppItem item;
        int digit;       // key that launches this row, 0 if none
    }

    private final List<Row> rows = new ArrayList<Row>();
    private final List<AppItem> gridItems = new ArrayList<AppItem>();
    private boolean grid;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(UI.screenBackground());
        getWindow().setStatusBarColor(0xFF0B0D10);
        getWindow().setNavigationBarColor(UI.NAVY_BOTTOM);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void toggleLayout() {
        Prefs.setLayout(this, grid ? Prefs.LAYOUT_LIST : Prefs.LAYOUT_GRID);
        build();
    }

    private void build() {
        grid = Prefs.layout(this) == Prefs.LAYOUT_GRID;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        AbsListView content;
        if (grid) {
            gridItems.clear();
            gridItems.addAll(Entries.resolveAll(this, Prefs.getList(this, Prefs.KEY_HOME_GRID)));
            appendMore(gridItems);
            GridView g = new GridView(this);
            g.setNumColumns(3);
            g.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            g.setPadding(0, UI.dp(this, 4), 0, 0);
            g.setAdapter(new GridAdapter());
            g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                    if (pos >= 0 && pos < gridItems.size()) Entries.launch(AppsActivity.this, gridItems.get(pos));
                }
            });
            content = g;
        } else {
            loadRows();
            ListView l = new ListView(this);
            l.setDivider(null);
            l.setDividerHeight(0);
            l.setAdapter(new ListAdapter());
            l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                    if (pos >= 0 && pos < rows.size() && rows.get(pos).item != null) {
                        Entries.launch(AppsActivity.this, rows.get(pos).item);
                    }
                }
            });
            content = l;
        }
        content.setSelector(new ColorDrawable(0));
        content.setVerticalScrollBarEnabled(false);
        content.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        content.requestFocus();
    }

    private void loadRows() {
        rows.clear();
        List<AppItem> top = Entries.resolveAll(this, Prefs.getList(this, Prefs.KEY_HOME_LIST));
        int n = 0;
        for (AppItem it : top) {
            n++;
            if ("std:tools".equals(it.id)) {
                Row h = new Row();
                h.header = n + " - " + it.label.toUpperCase();
                h.item = it;
                h.digit = n;
                rows.add(h);
                int m = 0;
                for (AppItem ch : Entries.resolveAll(this, Prefs.getList(this, Prefs.KEY_TOOLS))) {
                    m++;
                    Row r = new Row();
                    r.number = n + "." + m;
                    r.item = ch;
                    rows.add(r);
                }
            } else {
                Row r = new Row();
                r.number = String.valueOf(n);
                r.item = it;
                r.digit = n;
                rows.add(r);
            }
        }
        if (!hasMore(top)) {
            AppItem more = Entries.resolve(this, "std:more");
            if (more != null) {
                Row r = new Row();
                r.number = String.valueOf(++n);
                r.item = more;
                r.digit = n <= 9 ? n : 0;
                rows.add(r);
            }
        }
    }

    private static boolean hasMore(List<AppItem> list) {
        for (AppItem it : list) {
            if ("std:more".equals(it.id)) return true;
        }
        return false;
    }

    private void appendMore(List<AppItem> list) {
        if (hasMore(list)) return;
        AppItem more = Entries.resolve(this, "std:more");
        if (more != null) list.add(more);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SOFT_LEFT
                || keyCode == KeyEvent.KEYCODE_BOOKMARK) {
            toggleLayout();
            return true;
        }
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            int d = keyCode - KeyEvent.KEYCODE_0;
            if (grid) {
                if (d <= gridItems.size()) Entries.launch(this, gridItems.get(d - 1));
                return true;
            }
            for (Row r : rows) {
                if (r.digit == d && r.item != null) {
                    Entries.launch(this, r.item);
                    return true;
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    private TextView text(Context c, int sp, int color) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setSingleLine(true);
        return t;
    }

    private final class ListAdapter extends BaseAdapter {
        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int i) { return rows.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int i) { return rows.get(i).header != null ? 0 : 1; }
        @Override public boolean areAllItemsEnabled() { return false; }
        @Override public boolean isEnabled(int i) { return rows.get(i).header == null; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            Row r = rows.get(pos);
            Context c = AppsActivity.this;
            if (r.header != null) {
                LinearLayout h = (LinearLayout) convert;
                if (h == null) {
                    h = new LinearLayout(c);
                    h.setOrientation(LinearLayout.VERTICAL);
                    h.setLayoutParams(new AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 40)));
                    View rule = new View(c);
                    rule.setBackgroundColor(UI.DIVIDER);
                    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 1));
                    rp.leftMargin = UI.dp(c, 16);
                    rp.rightMargin = UI.dp(c, 16);
                    h.addView(rule, rp);
                    TextView t = text(c, 11, UI.GRAY_TEXT);
                    t.setLetterSpacing(0.15f);
                    t.setGravity(Gravity.CENTER_VERTICAL);
                    t.setPadding(UI.dp(c, 16), 0, 0, 0);
                    h.addView(t, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
                }
                ((TextView) h.getChildAt(1)).setText(r.header);
                return h;
            }
            LinearLayout row = (LinearLayout) convert;
            if (row == null) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(UI.listRowBackground());
                row.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 50)));
                TextView num = text(c, 13, UI.GRAY_TEXT);
                num.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                row.addView(num, new LinearLayout.LayoutParams(UI.dp(c, 41),
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                ImageView icon = new ImageView(c);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(UI.dp(c, 28), UI.dp(c, 28));
                ip.leftMargin = UI.dp(c, 6);
                row.addView(icon, ip);
                TextView name = text(c, 16, UI.WHITE);
                LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                np.leftMargin = UI.dp(c, 12);
                row.addView(name, np);
            }
            ((TextView) row.getChildAt(0)).setText(r.number);
            ((ImageView) row.getChildAt(1)).setImageDrawable(r.item.icon);
            ((TextView) row.getChildAt(2)).setText(r.item.label);
            return row;
        }
    }

    private final class GridAdapter extends BaseAdapter {
        @Override public int getCount() { return gridItems.size(); }
        @Override public Object getItem(int i) { return gridItems.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            Context c = AppsActivity.this;
            LinearLayout cell = (LinearLayout) convert;
            if (cell == null) {
                cell = new LinearLayout(c);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(UI.dp(c, 2), UI.dp(c, 12), UI.dp(c, 2), 0);
                cell.setBackground(UI.gridIconBackground(c));
                cell.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 99)));
                ImageView icon = new ImageView(c);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                cell.addView(icon, new LinearLayout.LayoutParams(UI.dp(c, 38), UI.dp(c, 38)));
                TextView label = text(c, 13, UI.WHITE);
                label.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = UI.dp(c, 8);
                cell.addView(label, lp);
            }
            AppItem it = gridItems.get(pos);
            ((ImageView) cell.getChildAt(0)).setImageDrawable(it.icon);
            ((TextView) cell.getChildAt(1)).setText(it.label);
            return cell;
        }
    }
}
