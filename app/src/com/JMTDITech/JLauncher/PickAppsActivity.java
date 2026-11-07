package com.JMTDITech.JLauncher;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Choose which entries appear in a list. The number beside an entry is its position;
 * entries appear in the order they are checked.
 */
public class PickAppsActivity extends Activity {
    static final String EXTRA_KEY = "key";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_DPAD = "dpad_key";

    private final List<AppItem> candidates = new ArrayList<AppItem>();
    private final List<String> picked = new ArrayList<String>();
    private AppAdapter adapter;
    private String key;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        key = getIntent().getStringExtra(EXTRA_KEY);
        if (key == null) key = Prefs.KEY_HOME_LIST;
        String t = getIntent().getStringExtra(EXTRA_TITLE);
        getWindow().setBackgroundDrawable(UI.screenBackground());
        getWindow().setStatusBarColor(UI.NAVY_TOP);
        getWindow().setNavigationBarColor(UI.NAVY_BOTTOM);

        picked.addAll(Prefs.getList(this, key));

        // Picked entries first (in order), then the rest.
        List<AppItem> all = new ArrayList<AppItem>();
        for (String name : Entries.STANDARD) {
            AppItem it = Entries.resolve(this, "std:" + name);
            if (it != null) all.add(it);
        }
        all.addAll(Entries.installedApps(this));
        Set<String> seen = new HashSet<String>();
        for (String id : picked) {
            for (AppItem it : all) {
                if (it.id.equals(id) && seen.add(id)) {
                    candidates.add(it);
                    break;
                }
            }
        }
        for (AppItem it : all) {
            if (seen.add(it.id)) candidates.add(it);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(UI.title(this, t == null ? "Choose apps" : t));

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(new ColorDrawable(0));
        list.setVerticalScrollBarEnabled(false);
        adapter = new AppAdapter(this, candidates, AppAdapter.STYLE_PICK);
        adapter.setPicked(picked);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                String entry = candidates.get(pos).id;
                if (getIntent().hasExtra(EXTRA_DPAD)) {
                    Prefs.setShortcut(PickAppsActivity.this, getIntent().getIntExtra(EXTRA_DPAD, 0), entry);
                    finish();
                    return;
                }
                if (!picked.remove(entry)) picked.add(entry);
                Prefs.setList(PickAppsActivity.this, key, picked);
                adapter.notifyDataSetChanged();
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        list.requestFocus();
    }
}
