package com.JMTDITech.JLauncher;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

/**
 * The Tools folder (editable numbered list) and the More folder (every installed app).
 * In grid layout, More is an icon grid; in list layout it is a text list.
 */
public class FolderActivity extends Activity {
    static final String EXTRA_FOLDER = "folder";
    private static final int MORE_GRID_COLUMNS = 3;

    private final List<AppItem> items = new ArrayList<AppItem>();
    private AppAdapter adapter;
    private boolean more;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        more = "more".equals(getIntent().getStringExtra(EXTRA_FOLDER));
        boolean iconGrid = more && Prefs.layout(this) == Prefs.LAYOUT_GRID;

        getWindow().setBackgroundDrawable(UI.screenBackground());
        getWindow().setStatusBarColor(UI.NAVY_TOP);
        getWindow().setNavigationBarColor(UI.NAVY_BOTTOM);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(UI.title(this, more ? "More" : "Tools"));

        AbsListView content;
        if (iconGrid) {
            GridView grid = new GridView(this);
            grid.setNumColumns(MORE_GRID_COLUMNS);
            grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            grid.setVerticalSpacing(UI.dp(this, 4));
            adapter = new AppAdapter(this, items, AppAdapter.STYLE_GRID);
            content = grid;
        } else {
            ListView list = new ListView(this);
            list.setDivider(null);
            list.setDividerHeight(0);
            adapter = new AppAdapter(this, items,
                    more ? AppAdapter.STYLE_TEXT : AppAdapter.STYLE_LIST);
            content = list;
        }
        content.setSelector(new ColorDrawable(0));
        content.setVerticalScrollBarEnabled(false);
        content.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content.setAdapter(adapter);
        content.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (pos >= 0 && pos < items.size()) Entries.launch(FolderActivity.this, items.get(pos));
            }
        });
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        content.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        items.clear();
        if (more) {
            items.addAll(Entries.installedApps(this));
        } else {
            items.addAll(Entries.resolveAll(this, Prefs.getList(this, Prefs.KEY_TOOLS)));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        load();
    }
}
