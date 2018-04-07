package com.skyline.terraexplorer.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnLoadFinishedListener;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.SettingsActivity;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.TEUnits;
import com.skyline.terraexplorer.models.UI;

public class SettingsTool extends BaseTool implements OnLoadFinishedListener {
    private LinearLayout exitUGView;

    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.title_activity_settings, R.drawable.settings, 40);
    }

    BroadcastReceiver broadcastReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            settingChanged(intent);
        }
    };

    public static IntentFilter SettingChanged = new IntentFilter("SettingChanged");
    public static final String SETTING_NAME = "com.skyline.terraexplorer.SETTING_NAME";
    public static final String SETTING_VALUE = "com.skyline.terraexplorer.SETTING_VALUE";
    public static final String PREFERENCES_NAME = "com.skyline.terraexplorer.Preferences";

    public SettingsTool() {
        LocalBroadcastManager.getInstance(TEApp.getAppContext()).registerReceiver(broadcastReciever, SettingChanged);

        SharedPreferences prefs = TEApp.getAppContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        setNavigationButtonsMode(prefs.getInt(TEApp.getAppContext().getString(R.string.key_navigation_buttons), 0));

        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().addOnLoadFinishedListener(SettingsTool.this);
            }
        });

        // prepare "exit underground" button
        exitUGView = new LinearLayout(TEApp.getAppContext());
        exitUGView.setVisibility(View.GONE);

        GradientDrawable closeButtonBackground = new GradientDrawable();
        closeButtonBackground.setCornerRadius(45 / 2);
        int borderWith = (int) UI.dp2px(TEApp.getAppContext().getResources().getDimension(R.dimen.border_width) * UI.scaleFactor());
        closeButtonBackground.setStroke(borderWith, TEApp.getAppContext().getResources().getColor(R.color.border_color));
        closeButtonBackground.setColor(TEApp.getAppContext().getResources().getColor(R.color.color_control_background) | 0xff000000);

        ImageButton exitUGButton = new ImageButton(TEApp.getAppContext());
        exitUGButton.setScaleType(ScaleType.FIT_CENTER);
        exitUGButton.setImageResource(R.drawable.close);
        int padding = (int) exitUGButton.getResources().getDimension(R.dimen.button_padding);
        exitUGButton.setPadding(padding, padding, padding, padding);
        exitUGButton.setBackgroundColor(exitUGButton.getResources().getColor(R.color.color_control_background));
        exitUGButton.setBackgroundDrawable(closeButtonBackground);
        exitUGView.addView(exitUGButton);

        TextView exitUGLabel = new TextView(TEApp.getAppContext());
        exitUGLabel.setText(R.string.settings_underground);
        exitUGLabel.setTextColor(Color.WHITE);
        exitUGLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, TEApp.getAppContext().getResources().getDimension(R.dimen.font_size_huge));
        exitUGLabel.setPadding(padding, 0, 0, 0);
        exitUGLabel.setGravity(Gravity.CENTER);
        exitUGView.addView(exitUGLabel, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        UI.getMainView().addView(exitUGView);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) exitUGView.getLayoutParams();
        lp.topMargin = (int) (20 * UI.scaleFactor());
        lp.leftMargin = (int) (20 * UI.scaleFactor());
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, -1);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, -1);

        exitUGButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exitUndergroundMode();
            }

        });


    }

    @Override
    public void open(Object param) {
        TEApp.getCurrentActivityContext().startActivity(new Intent(TEApp.getCurrentActivityContext(), SettingsActivity.class));
    }

    private void settingChanged(Intent intent) {
        int key = intent.getExtras().getInt(SETTING_NAME);
        final Object value = intent.getExtras().get(SETTING_VALUE);
        if (key == R.string.key_units) {
            TEUnits.instance.setUnitsType((Integer) value);
        } else if (key == R.string.key_navigation_buttons) {
            setNavigationButtonsMode((Integer) value);
        } else if (key == R.string.key_underground_button) {
            UI.runOnRenderThreadAsync(new Runnable() {
                @Override
                public void run() {
                    boolean isChecked = ISGWorld.getInstance().getCommand().IsChecked(1027, 0);
                    final Boolean val = (Boolean) value;
                    if (val == true && isChecked == false) // Underground should be on
                        ISGWorld.getInstance().getCommand().Execute(1027, 0);
                    if (val == false && isChecked == true) // Underground should be off
                        ISGWorld.getInstance().getCommand().Execute(1027, 0);
                    final boolean showButton = ISGWorld.getInstance().getCommand().IsChecked(1027, 0);
                    UI.runOnUiThreadAsync(new Runnable() {
                        @Override
                        public void run() {
                            if (showButton)
                                exitUGView.setVisibility(View.VISIBLE);
                            else
                                exitUGView.setVisibility(View.GONE);
                        }
                    });
                }
            });
        } else if (key == R.string.key_sunlight) {
            UI.runOnRenderThreadAsync(new Runnable() {
                @Override
                public void run() {
                    ISGWorld.getInstance().getCommand().Execute(1026, 0);
                }
            });
        }
    }

    private void setNavigationButtonsMode(final int mode) {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                int controls = ISGWorld.getInstance().getWindow().GetControls();
                if (mode == 1) // hide
                    controls &= 0xff;
                else
                    controls |= 0x100;
                ISGWorld.getInstance().getWindow().ShowControls(controls);
            }
        });
    }

    private void exitUndergroundMode() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                boolean isChecked = ISGWorld.getInstance().getCommand().IsChecked(1027, 0);
                if (isChecked)
                    ISGWorld.getInstance().getCommand().Execute(1027, 0);
            }
        });
        SharedPreferences prefs = TEApp.getAppContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(TEApp.getAppContext().getString(R.string.key_underground_button), false).apply();
        exitUGView.setVisibility(View.GONE);
    }

    @Override
    public void OnLoadFinished(boolean bSuccess) {
        SharedPreferences prefs = TEApp.getAppContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        TEUnits.instance.setUnitsType(prefs.getInt(TEApp.getAppContext().getString(R.string.key_units), 0));
        // clear underground mode. We start with underground mode off
        prefs.edit().putBoolean(TEApp.getAppContext().getString(R.string.key_underground_button), false).apply();
    }

}
