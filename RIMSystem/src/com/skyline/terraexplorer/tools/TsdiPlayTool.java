package com.skyline.terraexplorer.tools;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.skyline.teapi.ApiException;
import com.skyline.teapi.IPresentation;
import com.skyline.teapi.IPresentationStep;
import com.skyline.teapi.IProjectTree;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.teapi.PresentationStatus;
import com.skyline.teapi.PresentationStepType;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.ToolManager;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ToolContainer;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static com.skyline.rim.TeUtils.TeBase.getObjsFromProject;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_PRESENTATION_NEXT;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_PRESENTATION_PLAY;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_PRESENTATION_PRE;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_PRESENTATION_STEPS;

/**
 * Created by mft on 2017/8/23.
 */
public class TsdiPlayTool extends TsdiBaseTool implements ISGWorld.OnPresentationStatusChangedListener, ISGWorld.OnBeforePresentationItemActivationListener {
    private IPresentation currentPresentation = null;
    private String currentPresentationId = null;
    private int currentPresentationStatus = PresentationStatus.PS_NOTPLAYING;
    private ArrayList<String> presentationList = new ArrayList<String>();

    public TsdiPlayTool() {
    }

    public void OnBeforePresentationItemActivation(String PresentationID, IPresentationStep Step) {
//        String caption = UI.runOnRenderThread(new Callable<String>() {
//            @Override
//            public String call() throws Exception {
//                String caption = Step.getDescription();
//                return caption;
//            }
//        });
//        toolContainer.setUpperViewHidden(TextUtils.isEmpty(caption));
//        toolContainer.setText(caption);
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
        Log.i("tsdi", "TsdiPlayTool    OnPresentationStatusChangedUIThread");
        // if this is new presentation
        if (itemId.equals(currentPresentationId) == false) {
            // if we get stop on not current presentation, ignore it and do nothing.
            if (status == PresentationStatus.PS_NOTPLAYING)
                return;


            if (ToolContainer.INSTANCE.showWithDelegate(TsdiPlayTool.this) == false) {
                return;
            }

            //Log.i("tsdi", "PlayTool1");
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
                toolContainer.updateButton(8, R.drawable.pause, 0);
                break;
            case PresentationStatus.PS_NOTPLAYING:
                toolContainer.updateButton(8, R.drawable.play, 0);
                break;
            case PresentationStatus.PS_PAUSED:
            case PresentationStatus.PS_WAITINGCLICK:
                toolContainer.updateButton(8, R.drawable.play, 0);
                break;
        }
    }

    /***
     * 从project中检索全部的Presentation对象
     */
    public void initPresentation() {
        IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
        presentationList.clear();
        presentationList = getObjsFromProject(ObjectTypeCode.OT_PRESENTATION);
        //getPresentations(projectTree.getRootID(), "0",ObjectTypeCode.OT_PRESENTATION);
        if (presentationList.size() == 0)
            return;
        final String itemId = presentationList.get(0);
        currentPresentation = ISGWorld.getInstance().getCreator().GetObject(itemId).CastTo(IPresentation.class);
        currentPresentationId = itemId;
    }

    @Override
    protected void showNormalButtons() {
        super.showNormalButtons();
        toolContainer.addButton(TE_FUNCTION_BUTTON_PRESENTATION_PRE, R.drawable.rewind);
        toolContainer.addButton(TE_FUNCTION_BUTTON_PRESENTATION_PLAY, R.drawable.play);
        toolContainer.addButton(TE_FUNCTION_BUTTON_PRESENTATION_NEXT, R.drawable.forward);
        toolContainer.addButton(TE_FUNCTION_BUTTON_PRESENTATION_STEPS, R.drawable.list);
        Log.i("tsdi", "showNormalButtons");
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        // bug fix #18513. Close underground mode before starting presentation
        Intent intent = new Intent(SettingsTool.SettingChanged.getAction(0));
        intent.putExtra(SettingsTool.SETTING_NAME, R.string.key_underground_button);
        intent.putExtra(SettingsTool.SETTING_VALUE, false);
        LocalBroadcastManager.getInstance(TEApp.getAppContext()).sendBroadcast(intent);
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().addOnPresentationStatusChangedListener(TsdiPlayTool.this);
                //ISGWorld.getInstance().addOnBeforePresentationItemActivationListener(com.skyline.terraexplorer.tools.TsdiPlayTool.this);
                initPresentation();
            }
        });
        setCaption();
        return super.onBeforeOpenToolContainer();
    }

    // must perform all clean up on before close, since
    // steps screen calls Stop and then Play(stepIndex)
    // and since OnClosedToolcontainer fired after animation ended, the container is closed although presentation is playing.
    @Override
    public boolean onBeforeCloseToolContainer(ToolContainer.CloseReason closeReason) {
        if (currentPresentation != null) {
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    // if "close" is a result of switching to another presentation, do nothing to change current state
                    // bug fix 19006
                    if (currentPresentation.getPresentationStatus() != PresentationStatus.PS_BEFORE_SWITCHING_TO_ANOTHER_PRESENTATION)
                        currentPresentation.Pause();
                    ISGWorld.getInstance().removeOnPresentationStatusChangedListener(TsdiPlayTool.this);
                }
            });
            currentPresentation = null;
            currentPresentationId = null;
        }
        return true;
    }

    @Override
    public void onButtonClick(final int tag) {
        super.onButtonClick(tag);
        Log.i("tsdi", "TsdiPlayTool Click status=" + currentPresentationStatus);
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                switch (tag) {
                    case TE_FUNCTION_BUTTON_PRESENTATION_PRE:
                        try {
                            currentPresentation.PreviousStep();
                        } catch (Exception e) {
                            Log.i("tsdi", e.getMessage());
                        }
                        break;
                    case TE_FUNCTION_BUTTON_PRESENTATION_PLAY:
                        try {
                            if (currentPresentationStatus == PresentationStatus.PS_PAUSED) {
                                currentPresentation.Resume();
                            } else if (currentPresentationStatus == PresentationStatus.PS_WAITINGCLICK) {
                                currentPresentation.Continue();
                            } else if (currentPresentationStatus == PresentationStatus.PS_PLAYING) {
                                currentPresentation.Pause();
                            } else if (currentPresentationStatus == PresentationStatus.PS_NOTPLAYING) {
                                currentPresentation.Play(0);
                                currentPresentation.Continue();
                            }
                        } catch (Exception e) {
                            Log.i("tsdi", e.getMessage());
                        }
                        break;
                    case TE_FUNCTION_BUTTON_PRESENTATION_NEXT:
                        try {
                            currentPresentation.NextStep();
                        } catch (Exception e) {
                            Log.i("tsdi", e.getMessage());
                        }
                        break;
                    case TE_FUNCTION_BUTTON_PRESENTATION_STEPS:
                        try {
                            showKeySteps();
                        } catch (Exception e) {
                            Log.i("tsdi", e.getMessage());
                        }
                        break;
                }
            }
        });
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