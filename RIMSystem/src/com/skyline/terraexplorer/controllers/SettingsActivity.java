package com.skyline.terraexplorer.controllers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.DisplayGroupItem;
import com.skyline.terraexplorer.models.DisplayItem;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.TableDataSource;
import com.skyline.terraexplorer.models.TableDataSource.TableDataSourceDelegate;
import com.skyline.terraexplorer.models.TableDataSourceDelegateBase;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.tools.SettingsTool;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialogDelegateBase;

import java.util.Arrays;

public class SettingsActivity extends MatchParentActivity {

    private static final String ITEM_NAME = "com.skyline.terraexplrorer.ITEM_NAME";
    private static final String ITEM_TAG = "com.skyline.terraexplrorer.ITEM_TAG";
    private static final String ITEM_VALUE = "com.skyline.terraexplrorer.ITEM_VALUE";
    private static final String ITEM_VALUE_SET = "com.skyline.terraexplrorer.ITEM_VALUE_SET";
    private TableDataSourceDelegate delegate = new TableDataSourceDelegateBase() {
        @Override
        public void didSelectRowAtIndexPath(long packedPosition) {
            SettingsActivity.this.didSelectRowAtIndexPath(packedPosition);
        }
    };
    private TableDataSource datasource;

    private DisplayGroupItem project;
    private DisplayGroupItem view;
    private SharedPreferences prefs;
    //private DisplayItem websearchUrl;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UI.addHeader(R.string.title_activity_settings, R.drawable.settings, this);
        datasource = new TableDataSource(UI.addFullScreenTable(this), delegate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs = getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE);
        Intent startingIntent = getIntent();
        // if we have item name in starting intent, we need to show editor for that item
        if (startingIntent.getStringExtra(ITEM_NAME) != null) {
            showItemEditor(startingIntent);
        }
        // otherwise show regular settings screen
        else {
            showSettings();
        }

    }

    private void showItemEditor(Intent startingIntent) {
        // update header
        ((TextView) findViewById(R.id.header_title)).setText(startingIntent.getStringExtra(ITEM_NAME));
        // update list data
        DisplayGroupItem parentItem = new DisplayGroupItem(null);
        for (String string : startingIntent.getStringArrayExtra(ITEM_VALUE_SET)) {
            parentItem.childItems.add(new DisplayItem(string));
        }
        parentItem.childItems.get(startingIntent.getIntExtra(ITEM_VALUE, 0)).accessoryIcon = R.drawable.checkbox_on;
        datasource.setDataItems(new DisplayGroupItem[]{parentItem});
    }

    private void showSettings() {
        project = new DisplayGroupItem(null);

        project.childItems.addAll(Arrays.asList(new DisplayItem[]{
                        itemWithNameAndTag(R.string.settings_units, R.string.key_units),
//                        itemWithNameAndTag(R.string.settings_websearch, R.string.key_websearch),
//                        itemWithNameAndTag(R.string.settings_websearch_url, R.string.key_websearch_url),
//                        itemWithNameAndTag(R.string.settings_about, R.string.key_about),
//				itemWithNameAndTag(R.string.settings_tutorial, R.string.settings_tutorial)
                }
        ));
        //websearchUrl = project.childItems.get(2);

        view = new DisplayGroupItem(getString(R.string.settings_section_view));
        view.childItems.addAll(Arrays.asList(new DisplayItem[]{
                itemWithNameAndTag(R.string.settings_navigation_buttons, R.string.key_navigation_buttons),
                itemWithNameAndTag(R.string.settings_underground_button, R.string.key_underground_button),
                itemWithNameAndTag(R.string.settings_myposition, R.string.key_my_position),
                itemWithNameAndTag(R.string.settings_gpstrail, R.string.key_gps_trail),
                //itemWithNameAndTag(R.string.settings_sunlight, R.string.key_sunlight),
//                itemWithNameAndTag(R.string.menuButton_slideRight, R.string.key_menubutton_slide_right),
//                itemWithNameAndTag(R.string.menuButton_slideUp, R.string.key_menubutton_slide_up)
        }));
        for (int i = 0; i < project.childItems.size(); i++) {
            updateItemDisplay(project.childItems.get(i));
        }

        for (DisplayItem item : view.childItems) {
            updateItemDisplay(item);
        }
        datasource.setDataItems(new DisplayGroupItem[]{project, view});
    }

    private DisplayItem itemWithNameAndTag(int name, int tag) {
        DisplayItem item = new DisplayItem(name);
        item.tag = tag;
        return item;
    }

    private void updateItemDisplay(DisplayItem item) {
        String key = getString(item.tag);
        switch (item.tag) {
            case R.string.key_units:
                item.subTitle = prefs.getInt(key, 0) == 0 ? getString(R.string.settings_units_meters) : getString(R.string.settings_units_feet);
                break;
//            case R.string.key_websearch: {
//                boolean value = prefs.getBoolean(key, true);
//                item.subTitle = value ? getString(R.string.settings_websearch_on) : getString(R.string.settings_websearch_off);
//                item.accessoryIcon = value ? R.drawable.checkbox_on : R.drawable.checkbox_off;
//                websearchUrl.disabled = value == false;
//                break;
//            }
//            case R.string.key_websearch_url:
//                item.subTitle = prefs.getString(key, AppLinks.getDefaultSearchServer());
//                break;
//            case R.string.key_about:
//                try {
//                    PackageInfo pInfo = TEApp.getAppContext().getPackageManager().getPackageInfo(TEApp.getAppContext().getPackageName(), 0);
//                    String fullVersion = String.format(TEApp.getAppContext().getString(R.string.version_string), pInfo.versionName);
//                    item.subTitle = fullVersion;
//                } catch (NameNotFoundException e) {
//                    e.printStackTrace();
//                }
//                break;
            case R.string.key_navigation_buttons: {
                int value = prefs.getInt(key, 0);
                item.subTitle = value == 0 ? getString(R.string.settings_navigation_buttons_on) : getString(R.string.settings_navigation_buttons_off);
                break;
            }
            case R.string.key_underground_button: {
                boolean value = prefs.getBoolean(key, false);
                item.subTitle = value ? getString(R.string.settings_underground_button_on) : getString(R.string.settings_underground_button_off);
                item.accessoryIcon = value ? R.drawable.checkbox_on : R.drawable.checkbox_off;
                break;
            }
            case R.string.key_my_position: {
                boolean value = prefs.getBoolean(key, true);
                item.subTitle = value ? getString(R.string.settings_myposition_on) : getString(R.string.settings_myposition_off);
                item.accessoryIcon = value ? R.drawable.checkbox_on : R.drawable.checkbox_off;
                break;
            }
            case R.string.key_gps_trail: {
                boolean value = prefs.getBoolean(key, true);
                item.subTitle = value ? getString(R.string.settings_gpstrail_on) : getString(R.string.settings_gpstrail_off);
                item.accessoryIcon = value ? R.drawable.checkbox_on : R.drawable.checkbox_off;
                break;
            }
            case R.string.key_sunlight: {
                boolean value = prefs.getBoolean(key, true);
                item.subTitle = value ? getString(R.string.settings_sunlight_on) : getString(R.string.settings_sunlight_off);
                item.accessoryIcon = value ? R.drawable.checkbox_on : R.drawable.checkbox_off;
                break;
            }
//            case R.string.key_menubutton_slide_right: {
//                int value = prefs.getInt(key, 2);
//                item.subTitle = MainButtonDragGestures.instance.getActionNames()[value];
//                break;
//            }
//            case R.string.key_menubutton_slide_up: {
//                int value = prefs.getInt(key, 1);
//                item.subTitle = MainButtonDragGestures.instance.getActionNames()[value];
//                break;
//            }
        }
    }

    private void didSelectRowAtIndexPath(long packedPosition) {
        DisplayItem item = datasource.getItemForPath(packedPosition);
        // we in item edit mode
        if (item.tag == 0) {
            setResult(RESULT_OK, new Intent().putExtra(ITEM_VALUE, ExpandableListView.getPackedPositionChild(packedPosition)));
            finish();
            return;
        }

        String key = getString(item.tag);
        Object newValue = null;

        switch (item.tag) {
            case R.string.key_units: {
                showListEditor(item, new String[]{getString(R.string.settings_units_meters), getString(R.string.settings_units_feet)});
                break;
            }
//            case R.string.key_websearch_url: {
//                showTextEditor(R.string.settings_websearch_url, item);
//                break;
//            }

            case R.string.key_navigation_buttons: {
                showListEditor(item, new String[]{getString(R.string.settings_navigation_buttons_on), getString(R.string.settings_navigation_buttons_off)});
                break;
            }
            case R.string.key_underground_button:
//            case R.string.key_websearch:
            case R.string.key_my_position:
            case R.string.key_sunlight:
            case R.string.key_gps_trail: {
                prefs.edit().putBoolean(key, !prefs.getBoolean(key, true)).apply();
                newValue = prefs.getBoolean(key, true);
                break;
            }
//            case R.string.key_menubutton_slide_right: {
//                showListEditor(item, MainButtonDragGestures.instance.getActionNames(),
//                        prefs.getInt(key, 2));
//                break;
//            }
//            case R.string.key_menubutton_slide_up: {
//                showListEditor(item, MainButtonDragGestures.instance.getActionNames(),
//                        prefs.getInt(key, 1));
//                break;
//            }
        }
        if (newValue != null) {
            updateItemDisplay(item);
            datasource.reloadData();
            sendSettingChangedIntent(item.tag, newValue);
        }
    }

    private void showTextEditor(int title, final DisplayItem item) {
        ModalDialog dlg = new ModalDialog(title, new ModalDialogDelegateBase() {
            @Override
            public void modalDialogDidDismissWithOk(ModalDialog dlg) {
                prefs.edit().putString(getString(item.tag), dlg.getTextField().getText().toString()).apply();
                updateItemDisplay(item);
                sendSettingChangedIntent(item.tag, item.subTitle);
            }
        });
        dlg.setContentTextField(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, item.subTitle);
        dlg.show();
    }

    private void sendSettingChangedIntent(int settingTag, Object newValue) {
        Intent intent = new Intent(SettingsTool.SettingChanged.getAction(0));
        intent.putExtra(SettingsTool.SETTING_NAME, settingTag);
        if (newValue instanceof Boolean)
            intent.putExtra(SettingsTool.SETTING_VALUE, (Boolean) newValue);
        else if (newValue instanceof Integer)
            intent.putExtra(SettingsTool.SETTING_VALUE, (Integer) newValue);
        else if (newValue instanceof String)
            intent.putExtra(SettingsTool.SETTING_VALUE, (String) newValue);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void showListEditor(DisplayItem item, String[] values) {
        showListEditor(item, values, -1);
    }

    private void showListEditor(DisplayItem item, String[] values, int currentValue) {

        if (currentValue == -1)
            currentValue = prefs.getInt(getString(item.tag), 0);

        Intent intent = new Intent(this, this.getClass());
        intent.putExtra(ITEM_NAME, item.name)
                .putExtra(ITEM_TAG, item.tag)
                .putExtra(ITEM_VALUE, currentValue)
                .putExtra(ITEM_VALUE_SET, values);
        startActivityForResult(intent, item.tag);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            prefs.edit().putInt(getString(requestCode), data.getIntExtra(ITEM_VALUE, 0)).apply();
            sendSettingChangedIntent(requestCode, data.getIntExtra(ITEM_VALUE, 0));
        }
    }
}
