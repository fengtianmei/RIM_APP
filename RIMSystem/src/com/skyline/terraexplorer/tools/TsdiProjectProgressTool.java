package com.skyline.terraexplorer.tools;

import android.app.ActionBar;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.DatePicker;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.skyline.rim.utils.DateUtil;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialogDelegateBase;
import com.skyline.terraexplorer.views.ToolContainer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_DATE_PICKER;

/**
 * Created by mft on 2017/8/23.
 */

public class TsdiProjectProgressTool extends BaseToolWithContainer {
    private Object originalFixedLocalTime;
    private HorizontalScrollView toolContainerHorizontalScrollView;
    private Calendar calendar;
    private String string_start = "2016-10-24";
    private String string_end = "2018-11-20";
    Date date_start = null;
    Date date_end = null;


    private ModalDialogDelegateBase modalDelegate = new ModalDialogDelegateBase() {
        public void modalDialogDidDismissWithOk(ModalDialog dlg) {
            TsdiProjectProgressTool.this.modalDialogDidDismissWithOk(dlg);
        }

        ;
    };
    // slider goes from 6 am to 10 pm
    private static int DATE_PICKER_TAG = 345346345;

    public TsdiProjectProgressTool() {
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
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

        SeekBar slider = new SeekBar(TEApp.getAppContext());
        slider.setProgressDrawable(slider.getContext().getResources().getDrawable(R.drawable.seek_bar));
        slider.setThumb(slider.getContext().getResources().getDrawable(R.drawable.seek_bar_thumb));
        slider.setPadding(20, 0, 20, 0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date_start = sdf.parse(string_start);
            date_end = sdf.parse(string_end);
        } catch (java.text.ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int length = DateUtil.getGapCount(date_start, date_end);
        slider.setMax(length);
        slider.setProgress(0);
        slider.setOnTouchListener(touchListener);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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

        LinearLayout wrapper = new LinearLayout(slider.getContext());
        float buttonSize = slider.getContext().getResources().getDimension(R.dimen.button_size);
        wrapper.addView(slider, new LinearLayout.LayoutParams((int) (4.5 * buttonSize - 4), ActionBar.LayoutParams.WRAP_CONTENT));
        wrapper.setGravity(Gravity.CENTER);
        toolContainer.addViewWithSeparator(wrapper);
        toolContainer.addButton(TE_FUNCTION_BUTTON_DATE_PICKER, R.drawable.date);
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
        super.onButtonClick(tag);
        switch (tag) {
            case TE_FUNCTION_BUTTON_DATE_PICKER: {
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
        SimpleDateFormat sdf1;
        sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        String text = sdf1.format(calendar.getTime());
        toolContainer.setText(text);
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    private void sliderMoved(int progress) {
        calendar.setTime(date_start);
        Date newDate2 = calendar.getTime();
        newDate2.setTime(date_start.getTime() + (long) progress * 24 * 60 * 60 * 1000);
        calendar.setTime(newDate2);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateOk = simpleDateFormat.format(newDate2);
        updateText();
    }

    private void sliderMoveEnded() {

    }

    @Override
    public boolean onBeforeCloseToolContainer(ToolContainer.CloseReason closeReason) {

        return true;
    }

    private void modalDialogDidDismissWithOk(ModalDialog dlg) {
        DatePicker datePicker = (DatePicker) dlg.contentViewWithTag(DATE_PICKER_TAG);
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
        updateText();
    }
}
