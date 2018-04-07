package com.skyline.terraexplorer.tools;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.skyline.teapi.ISGWorld;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

public abstract class ProgressTool extends BaseToolWithContainer {
    private static final int CANCEL_TAG = 100;

    private TextView progressLabel;
    protected boolean workCanceled;

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        cancelWork();
        return true;
    }

    @Override
    public void onButtonClick(int tag) {
        if (tag == CANCEL_TAG)
            cancelWork();
    }

    public void doWorkAsync() {
        showProgressButtons();
        workCanceled = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                UI.getTEView().disableRender();
                doWork();
                UI.getTEView().enableRender();
                UI.runOnUiThreadAsync(new Runnable() {
                    @Override
                    public void run() {
                        progressLabel = null;
                        if (toolContainer != null) {
                            showNormalButtons();
                            workCompleted();
                        }
                    }
                });
            }
        }).start();
    }

    abstract protected void doWork();

    abstract protected void showNormalButtons();

    abstract protected void workCompleted();

    private void showProgressButtons() {
        toolContainer.removeButtons();
        Context ctx = toolContainer.getContext();
        TextView label = new TextView(ctx);
        label.setText("  0%");
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, ctx.getResources().getDimension(R.dimen.font_size_normal));
        label.setGravity(Gravity.CENTER);
        label.setPadding(10, 0, 10, 0);
        toolContainer.addView(label);
        toolContainer.addButton(CANCEL_TAG, R.drawable.stop_calc, R.string.cancel);

        progressLabel = label;
    }

    protected void setProgress(int progress, int range) {
        final int percentDone = (int) (progress * 1.0 / range * 100);
        // probably will arrive not on UI thread
        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                if (toolContainer != null) {
                    progressLabel.setText(String.format("%3d%%", percentDone));
                }
            }
        });
    }

    protected void cancelWork() {
        workCanceled = true;
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().SetParam(8045, 0);
            }
        });
    }
}
