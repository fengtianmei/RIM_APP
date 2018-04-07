package com.skyline.terraexplorer.tools;

import android.app.ActionBar.LayoutParams;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewParent;
import android.widget.DatePicker;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.skyline.teapi.IPosition;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.SliderDisplayMode;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialogDelegateBase;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Callable;

public class ShadowTool extends BaseToolWithContainer {
    private Object originalFixedLocalTime;
    private HorizontalScrollView toolContainerHorizontalScrollView;
    private Calendar calendar;
    private ModalDialogDelegateBase modalDelegate = new ModalDialogDelegateBase() {
        public void modalDialogDidDismissWithOk(ModalDialog dlg) {
            ShadowTool.this.modalDialogDidDismissWithOk(dlg);
        }

        ;
    };
    // slider goes from 6 am to 10 pm
    private static final int SLIDER_MAX_VALUE = (16 * 60 - 1);
    private static int DATE_PICKER_TAG = 345346345;

    public ShadowTool() {
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.mm_analyze_shadow, R.drawable.shadow, MenuEntry.MenuEntryAnalyze(), 36);
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Disallow ScrollView to intercept touch events.
                    toolContainerHorizontalScrollView.requestDisallowInterceptTouchEvent(true);
                    break;

                case MotionEvent.ACTION_UP:
                    // Allow ScrollView to intercept touch events.
                    toolContainerHorizontalScrollView.requestDisallowInterceptTouchEvent(false);
                    break;
            }

            // Handle slider touch events.
            v.onTouchEvent(event);
            return true;
        }
    };

    @Override
    public boolean onBeforeOpenToolContainer() {
        // turn on shadow
        Boolean isShadowSupported = UI.runOnRenderThread(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Short isShadowSupported = (Short) ISGWorld.getInstance().GetParam(8380);
                if (isShadowSupported == 0)
                    return false;
                // turn on shadow
                if (ISGWorld.getInstance().getCommand().IsChecked(2118, 0) == false)
                    ISGWorld.getInstance().getCommand().Execute(2118, 0);
                // turn on sun
                if (ISGWorld.getInstance().getCommand().IsChecked(1026, 0) == false)
                    ISGWorld.getInstance().getCommand().Execute(1026, 0);
                originalFixedLocalTime = ISGWorld.getInstance().getDateTime().getFixedLocalTime();
                ISGWorld.getInstance().getDateTime().SetMode(SliderDisplayMode.MODE_TIME);
                return true;
            }
        });
        if (isShadowSupported == false) {
            ModalDialog dlg = new ModalDialog(R.string.shadow_tool_shadow_not_supported_title, new ModalDialogDelegateBase());
            dlg.setContentMessage(R.string.shadow_tool_shadow_not_supported);
            dlg.show();
            return false;
        }
        SeekBar slider = new SeekBar(TEApp.getAppContext());
        slider.setProgressDrawable(slider.getContext().getResources().getDrawable(R.drawable.seek_bar));
        slider.setThumb(slider.getContext().getResources().getDrawable(R.drawable.seek_bar_thumb));
        slider.setPadding(20, 0, 20, 0);
        slider.setMax(SLIDER_MAX_VALUE);
        slider.setOnTouchListener(touchListener);
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sliderMoveEnded();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                sliderMoved(progress);
            }
        });
        slider.setProgress((calendar.get(Calendar.HOUR_OF_DAY) - 6) * 60 + calendar.get(Calendar.MINUTE));
        LinearLayout wrapper = new LinearLayout(slider.getContext());
        float buttonSize = slider.getContext().getResources().getDimension(R.dimen.button_size);
        wrapper.addView(slider, new LinearLayout.LayoutParams((int) (4.5 * buttonSize - 4), LayoutParams.WRAP_CONTENT));
        wrapper.setGravity(Gravity.CENTER);
        toolContainer.addViewWithSeparator(wrapper);
        toolContainer.addButton(1, R.drawable.date);
        ViewParent parent = wrapper.getParent();
        while (parent != null) {
            if (parent instanceof HorizontalScrollView) {
                toolContainerHorizontalScrollView = (HorizontalScrollView) parent;
                break;
            }
            parent = parent.getParent();
        }
        updateText();
        return true;
    }

    @Override
    public void onButtonClick(int tag) {
        switch (tag) {
            case 1: {
                // show date dialog
                ModalDialog dlg = new ModalDialog(R.string.shadow_tool_choose_date, modalDelegate);
                DatePicker datePicker = new DatePicker(TEApp.getAppContext());
                //datePicker.setBackgroundColor(0xff404040);
                datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), null);
                datePicker.setCalendarViewShown(false);
                datePicker.setTag(DATE_PICKER_TAG);
                dlg.setContent(datePicker);
                //datePicker.frame = CGRectMake(0, 0, 400 * [TEAUI scaleFactor], 400 * [TEAUI scaleFactor]);
                //[datePicker roundCorners:5 withColor:[UIColor clearColor]];
                dlg.show();
                break;
            }
        }
    }

    private void updateText() {
        String dateString = DateFormat.getLongDateFormat(TEApp.getAppContext()).format(calendar.getTime());
        String timeString = DateFormat.getTimeFormat(TEApp.getAppContext()).format(calendar.getTime());
        String text = String.format("%s\r\n%s", timeString, dateString);
        toolContainer.setText(text);

        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                //convert to UTC
                TimeZone tz = calendar.getTimeZone();
                int offsetFromLocalTimeToGMTinSec = tz.getOffset(calendar.getTimeInMillis()) / 1000;

                // Convert to user location local time
                IPosition pos = ISGWorld.getInstance().getNavigate().GetPosition();
                double hourOffset = ((24.0) / 360) * pos.getX() * (-1);
                int OffsetFromGMTToCurrentPositionsInSec = (int) (hourOffset * 60.0 * 60.0);
                Calendar cal = (Calendar) calendar.clone();
                cal.add(Calendar.SECOND, OffsetFromGMTToCurrentPositionsInSec + offsetFromLocalTimeToGMTinSec);
                final Date date = cal.getTime();
                ISGWorld.getInstance().getDateTime().setCurrent(date);
            }
        });
    }

    private void sliderMoved(int progress) {
        // disable shadow while moving slider
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().SetParam(8210, 0);
            }
        });
        // slider value is 16 hours, i.e 16*60 seconds
        float selectedSeconds = progress;
        int hours = (int) Math.floor(selectedSeconds / 60.0);
        int minutes = (int) Math.floor(selectedSeconds - hours * 60);
        calendar.set(Calendar.HOUR_OF_DAY, (hours + 6) % 24);
        calendar.set(Calendar.MINUTE, minutes);
        updateText();
    }

    private void sliderMoveEnded() {
        // enable shadow after moved finished
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().SetParam(8210, 1);
            }
        });
    }

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        // turn off shadow
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                // turn off shadow
                if (ISGWorld.getInstance().getCommand().IsChecked(2118, 0))
                    ISGWorld.getInstance().getCommand().Execute(2118, 0);
                // turn off sun
                if (ISGWorld.getInstance().getCommand().IsChecked(1026, 0))
                    ISGWorld.getInstance().getCommand().Execute(1026, 0);

                ISGWorld.getInstance().getDateTime().SetMode(SliderDisplayMode.MODE_FIXED_TIME);
                ISGWorld.getInstance().getDateTime().setFixedLocalTime(originalFixedLocalTime);
            }
        });
        originalFixedLocalTime = null;
        return true;
    }

    private void modalDialogDidDismissWithOk(ModalDialog dlg) {
        DatePicker datePicker = (DatePicker) dlg.contentViewWithTag(DATE_PICKER_TAG);
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
        updateText();
    }
}
