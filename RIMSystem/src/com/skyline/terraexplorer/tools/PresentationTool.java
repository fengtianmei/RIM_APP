package com.skyline.terraexplorer.tools;

import android.content.Intent;
import android.text.TextUtils;

import com.skyline.teapi.ApiException;
import com.skyline.teapi.IPresentation;
import com.skyline.teapi.IPresentationStep;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnPresentationStatusChangedListener;
import com.skyline.teapi.PresentationStatus;
import com.skyline.teapi.PresentationStepType;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.ToolManager;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ToolContainer;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

import java.util.concurrent.Callable;

public class PresentationTool extends BaseToolWithContainer implements OnPresentationStatusChangedListener {
    private IPresentation currentPresentation = null;
    private String currentPresentationId = null;
    private int currentPresentationStatus = PresentationStatus.PS_NOTPLAYING;

    public PresentationTool() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().addOnPresentationStatusChangedListener(PresentationTool.this);
            }
        });
    }

    public void OnPresentationStatusChanged(final String itemId, final int status) {
        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                OnPresentationStatusChangedUIThread(itemId, status);
            }
        });
    }

    private void OnPresentationStatusChangedUIThread(final String itemId, int status) {
        // if this is new presentation
        if (itemId.equals(currentPresentationId) == false) {
            // if we get stop on not current presentation, ignore it and do nothing.
            if (status == PresentationStatus.PS_NOTPLAYING)
                return;

            if (ToolContainer.INSTANCE.showWithDelegate(PresentationTool.this) == false) {
                return;
            }

            // bug  fix #19679. When playing presentation, we open another project.
            // onbeforeclose calls pause on presentation. Tools receives the event and passes it to main thread
            // at the same time, MainActivity already queued call to open on render thread.
            // We start to execute this function on main thread, while render thread started the load process
            // causing the presentation to unload-> exception object not found. Solution: add try/catch
            try {
                currentPresentation = UI.runOnRenderThread(new Callable<IPresentation>() {

                    @Override
                    public IPresentation call() throws Exception {
                        return ISGWorld.getInstance().getCreator().GetObject(itemId).CastTo(IPresentation.class);
                    }
                });
                currentPresentationId = itemId;
            } catch (ApiException e) {
                return;
            }

        }
        setCaption();
        currentPresentationStatus = status;
        switch (status) {
            case PresentationStatus.PS_PLAYING:
                toolContainer.updateButton(2, R.drawable.pause, 0);
                break;
            case PresentationStatus.PS_NOTPLAYING:
                toolContainer.hideAndClearDelegate();
                break;
            case PresentationStatus.PS_PAUSED:
            case PresentationStatus.PS_WAITINGCLICK:
                toolContainer.updateButton(2, R.drawable.play, 0);
                break;
        }
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        // bug fix #18513. Close underground mode before starting presentation
        Intent intent = new Intent(SettingsTool.SettingChanged.getAction(0));
        intent.putExtra(SettingsTool.SETTING_NAME, R.string.key_underground_button);
        intent.putExtra(SettingsTool.SETTING_VALUE, false);
        LocalBroadcastManager.getInstance(TEApp.getAppContext()).sendBroadcast(intent);

        setCaption();
        toolContainer.addButton(1, R.drawable.rewind);
        toolContainer.addButton(2, R.drawable.pause);
        toolContainer.addButton(3, R.drawable.forward);
        toolContainer.addButton(4, R.drawable.list);
        return true;
    }

    @Override
    public void onButtonClick(final int tag) {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                switch (tag) {
                    case 1:
                        currentPresentation.PreviousStep();
                        break;
                    case 2:
                        if (currentPresentationStatus == PresentationStatus.PS_PAUSED) {
                            currentPresentation.Resume();
                        } else if (currentPresentationStatus == PresentationStatus.PS_WAITINGCLICK) {
                            currentPresentation.Continue();
                        } else if (currentPresentationStatus == PresentationStatus.PS_PLAYING) {
                            currentPresentation.Pause();
                        }
                        break;
                    case 3:
                        currentPresentation.NextStep();
                        break;
                    case 4:
                        showKeySteps();
                        break;
                }
            }
        });
    }

    // must perform all clean up on before close, since
    // steps screen calls Stop and then Play(stepIndex)
    // and since OnClosedToolcontainer fired after animation ended, the container is closed although presentation is playing.
    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        if (currentPresentation != null) {
            if (closeReason == CloseReason.CloseButton) {
                UI.runOnRenderThread(new Runnable() {
                    @Override
                    public void run() {
                        currentPresentation.Stop();
                        currentPresentation.ResetPresentation();
                    }
                });
            } else {
                UI.runOnRenderThread(new Runnable() {
                    @Override
                    public void run() {
                        // if "close" is a result of switching to another presentation, do nothing to change current state
                        // bug fix 19006
                        if (currentPresentation.getPresentationStatus() != PresentationStatus.PS_BEFORE_SWITCHING_TO_ANOTHER_PRESENTATION)
                            currentPresentation.Pause();
                    }
                });
            }
            currentPresentation = null;
            currentPresentationId = null;
        }
        return true;
    }

    private void setCaption() {
        if (currentPresentation == null)
            return;
        String caption = UI.runOnRenderThread(new Callable<String>() {
            @Override
            public String call() throws Exception {
                int stepIndex = currentPresentation.getSteps().getCurrent();
                String caption = null;
                while (stepIndex >= 0) {
                    IPresentationStep step = ((TEIUnknownHandle) currentPresentation.getSteps().get_Step(stepIndex)).CastTo(IPresentationStep.class);
                    if (step.getType() == PresentationStepType.ST_CAPTION || step.getType() == PresentationStepType.ST_CLEARCAPTION) {
                        caption = step.getCaptionText();
                        break;
                    }
                    stepIndex--;
                }
                return caption;
            }
        });
        toolContainer.setUpperViewHidden(TextUtils.isEmpty(caption));
        toolContainer.setText(caption);
    }

    private void showKeySteps() {
        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                ToolManager.INSTANCE.openTool(PresentationStepsTool.class.getName(), currentPresentationId);
            }
        });
    }
}
