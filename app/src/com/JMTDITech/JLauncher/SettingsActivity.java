package com.JMTDITech.JLauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.KeyEvent;
import java.util.List;
import android.widget.Toast;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/** JLauncher settings. This is also the app's launcher-icon entry point. */
public class SettingsActivity extends Activity {
    private static final int REQ_HOME_ROLE = 1;

    private static final int ROW_LAYOUT = 0;
    private static final int ROW_HOME_LIST = 1;
    private static final int ROW_HOME_GRID = 2;
    private static final int ROW_TOOLS = 3;
    private static final int ROW_MENU_KEY = 4;
    private static final int ROW_CLOSE_PKGS = 5;
    private static final int ROW_ACCESSIBILITY = 6;
    private static final int ROW_DEFAULT = 7;
    private static final int ROW_RESET = 8;
    private static final int ROW_COUNT = 9;

    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(UI.screenBackground());
        getWindow().setStatusBarColor(UI.NAVY_TOP);
        getWindow().setNavigationBarColor(UI.NAVY_BOTTOM);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(UI.title(this, "JLauncher"));

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(new ColorDrawable(0));
        list.setVerticalScrollBarEnabled(false);
        adapter = new RowAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                onRow(pos);
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        list.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    private void onRow(int row) {
        switch (row) {
            case ROW_LAYOUT:
                Prefs.setLayout(this, Prefs.layout(this) == Prefs.LAYOUT_LIST
                        ? Prefs.LAYOUT_GRID : Prefs.LAYOUT_LIST);
                break;
            case ROW_HOME_LIST:
                pick(Prefs.KEY_HOME_LIST, "Home list");
                break;
            case ROW_HOME_GRID:
                pick(Prefs.KEY_HOME_GRID, "Home grid");
                break;
            case ROW_TOOLS:
                pick(Prefs.KEY_TOOLS, "Tools folder");
                break;
            case ROW_MENU_KEY:
                captureMenuKey();
                break;
            case ROW_CLOSE_PKGS:
                editClosePackages();
                break;
            case ROW_ACCESSIBILITY:
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                break;
            case ROW_DEFAULT:
                requestDefaultLauncher();
                break;
            case ROW_RESET:
                new AlertDialog.Builder(this)
                        .setTitle("Reset JLauncher?")
                        .setMessage("Restore the default layout and app lists.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                Prefs.resetAll(SettingsActivity.this);
                                adapter.notifyDataSetChanged();
                            }
                        })
                        .show();
                break;
            default:
                break;
        }
        adapter.notifyDataSetChanged();
    }

    /** Learn a physical key: press it while the dialog is showing. */
    private void captureMenuKey() {
        final AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Quick menu key")
                .setMessage("Press the key you want to open the quick menu.\n\n"
                        + "Number keys, * and #, the D-pad and BACK can't be assigned.")
                .setNeutralButton("Use default", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface di, int w) {
                        Prefs.clearMenuKey(SettingsActivity.this);
                        adapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        d.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface di, int keyCode, KeyEvent e) {
                if (e.getAction() != KeyEvent.ACTION_DOWN || e.getRepeatCount() != 0) return false;
                if (!Prefs.isAssignable(e)) return false;
                Prefs.setMenuKey(SettingsActivity.this, e);
                di.dismiss();
                adapter.notifyDataSetChanged();
                Toast.makeText(SettingsActivity.this,
                        "Saved: " + Prefs.describeMenuKey(SettingsActivity.this),
                        Toast.LENGTH_LONG).show();
                return true;
            }
        });
        d.show();
    }

    private void editClosePackages() {
        final EditText input = new EditText(this);
        input.setText(TextUtils.join(", ", Prefs.closePackages(this)));
        new AlertDialog.Builder(this)
                .setTitle("Close app opened by menu key")
                .setMessage("If the phone also opens one of these apps when you press the "
                        + "quick-menu key, JLauncher sends you back home, where the menu stays open. "
                        + "Package names, separated by commas. Needs the accessibility service.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        Prefs.setClosePackages(SettingsActivity.this, input.getText().toString());
                        adapter.notifyDataSetChanged();
                    }
                })
                .show();
    }

    private void pick(String key, String title) {
        startActivity(new Intent(this, PickAppsActivity.class)
                .putExtra(PickAppsActivity.EXTRA_KEY, key)
                .putExtra(PickAppsActivity.EXTRA_TITLE, title));
    }

    private boolean isDefaultLauncher() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null
                && getPackageName().equals(ri.activityInfo.packageName);
    }

    private void requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                RoleManager rm = (RoleManager) getSystemService(Context.ROLE_SERVICE);
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)
                        && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQ_HOME_ROLE);
                    return;
                }
            } catch (RuntimeException ignored) {
                // fall through to the system Home settings screen
            }
        }
        startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        adapter.notifyDataSetChanged();
    }

    private String titleFor(int row) {
        switch (row) {
            case ROW_LAYOUT: return "Home layout";
            case ROW_HOME_LIST: return "Edit home list";
            case ROW_HOME_GRID: return "Edit home grid";
            case ROW_TOOLS: return "Edit Tools folder";
            case ROW_MENU_KEY: return "Quick menu key";
            case ROW_CLOSE_PKGS: return "Close app opened by menu key";
            case ROW_ACCESSIBILITY: return "Accessibility service";
            case ROW_DEFAULT: return "Default launcher";
            default: return "Reset to defaults";
        }
    }

    private String valueFor(int row) {
        switch (row) {
            case ROW_LAYOUT:
                return Prefs.layout(this) == Prefs.LAYOUT_GRID ? "Grid (tap to switch to List)"
                        : "List (tap to switch to Grid)";
            case ROW_HOME_LIST:
                return Prefs.getList(this, Prefs.KEY_HOME_LIST).size() + " entries";
            case ROW_HOME_GRID:
                return Prefs.getList(this, Prefs.KEY_HOME_GRID).size() + " entries";
            case ROW_TOOLS:
                return Prefs.getList(this, Prefs.KEY_TOOLS).size() + " entries";
            case ROW_MENU_KEY:
                return Prefs.describeMenuKey(this);
            case ROW_CLOSE_PKGS: {
                List<String> l = Prefs.closePackages(this);
                return l.isEmpty() ? "None" : TextUtils.join(", ", l);
            }
            case ROW_ACCESSIBILITY:
                return NotificationService.isConnected()
                        ? "On"
                        : "Off. Needed for the menu key: it lets JLauncher send you back from the phone's own action (e.g. Contacts)";
            case ROW_DEFAULT:
                return isDefaultLauncher() ? "JLauncher is your home app" : "Tap to make JLauncher your home app";
            default:
                return "";
        }
    }

    private final class RowAdapter extends BaseAdapter {
        @Override public int getCount() { return ROW_COUNT; }
        @Override public Object getItem(int position) { return position; }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View cv, ViewGroup parent) {
            LinearLayout row;
            TextView name;
            TextView value;
            if (cv == null) {
                row = new LinearLayout(SettingsActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(UI.listRowBackground());
                int pad = UI.dp(SettingsActivity.this, 14);
                row.setPadding(pad, UI.dp(SettingsActivity.this, 8), pad, UI.dp(SettingsActivity.this, 8));
                row.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(SettingsActivity.this, 64)));

                name = new TextView(SettingsActivity.this);
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                name.setTextColor(UI.listRowText());
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                name.setDuplicateParentStateEnabled(true);
                row.addView(name);

                value = new TextView(SettingsActivity.this);
                value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                value.setTextColor(UI.listRowText());
                value.setMaxLines(2);
                value.setEllipsize(TextUtils.TruncateAt.END);
                value.setAlpha(0.7f);
                value.setDuplicateParentStateEnabled(true);
                row.addView(value);

                row.setTag(new TextView[] {name, value});
            } else {
                row = (LinearLayout) cv;
            }
            TextView[] tv = (TextView[]) row.getTag();
            tv[0].setText(titleFor(position));
            tv[1].setText(valueFor(position));
            tv[1].setVisibility(valueFor(position).length() == 0 ? View.GONE : View.VISIBLE);
            return row;
        }
    }
}
