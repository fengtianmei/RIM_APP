package com.skyline.terraexplorer.controllers;

import android.os.Bundle;
import android.text.Html;

import com.skyline.teapi.IPresentation;
import com.skyline.teapi.IPresentationStep;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.DisplayGroupItem;
import com.skyline.terraexplorer.models.DisplayItem;
import com.skyline.terraexplorer.models.TableDataSource;
import com.skyline.terraexplorer.models.TableDataSourceDelegateBase;
import com.skyline.terraexplorer.models.UI;

import java.util.concurrent.Callable;

public class PresentationStepsActivity extends MatchParentActivity {
    public static final String PRESENTATION_ID = "com.skyline.terraexplorer.PRESENTATION_ID";
    private TableDataSource dataSource;
    private String presentationId;

    private TableDataSourceDelegateBase delegate = new TableDataSourceDelegateBase() {
        public void didSelectRowAtIndexPath(long packedPosition) {
            PresentationStepsActivity.this.didSelectRowAtIndexPath(packedPosition);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UI.addHeader(R.string.title_activity_presentation_steps, R.drawable.list, this);
        dataSource = new TableDataSource(UI.addFullScreenTable(this), delegate);

        presentationId = getIntent().getExtras().getString(PRESENTATION_ID);
        DisplayGroupItem steps = UI.runOnRenderThread(new Callable<DisplayGroupItem>() {

            @Override
            public DisplayGroupItem call() throws Exception {
                return getPresentationSteps();
            }
        });
        dataSource.setDataItems(new DisplayGroupItem[]{steps});
    }

    private DisplayGroupItem getPresentationSteps() {
        IPresentation presentation = ISGWorld.getInstance().getCreator().GetObject(presentationId).CastTo(IPresentation.class);
        int stepsCount = presentation.getSteps().getCount();
        DisplayGroupItem steps = new DisplayGroupItem(null);
        for (int i = 0; i < stepsCount; i++) {
            IPresentationStep step = ((TEIUnknownHandle) presentation.getSteps().get_Step(i)).CastTo(IPresentationStep.class);
            String stepName = String.format("%d. %s", i, step.getDescription());
            DisplayItem item = new DisplayItem(stepName);
            if (step.getKeyStep()) {
                item.attributedName = Html.fromHtml("<b>" + stepName + "<b>");
            }
            item.tag = i;
            steps.childItems.add(item);
        }
        return steps;
    }

    private void didSelectRowAtIndexPath(long packedPosition) {
        final DisplayItem item = dataSource.getItemForPath(packedPosition);
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                IPresentation presentation = ISGWorld.getInstance().getCreator().GetObject(presentationId).CastTo(IPresentation.class);
                presentation.Stop();
                presentation.Play(item.tag);
            }
        });
        UI.popToMainActivity();
    }
}
