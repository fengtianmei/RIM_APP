package com.skyline.terraexplorer.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.skyline.teapi.ApiException;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.IFeatureAttribute;
import com.skyline.teapi.IPopupMessage;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerraExplorerMessage;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.MsgType;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.FeatureAttributesActivity;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.models.UI.HeaderOptions;

import java.util.EnumSet;

public class MessageView extends FrameLayout {

    private WebView webView;
    private View fullViewHeader;
    private View toggleButton;
    private String displayedMessageId;

    private class MyWebClient extends WebViewClient {
        boolean openInExternalBrowser = false;

        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (openInExternalBrowser == false)
                return super.shouldOverrideUrlLoading(view, url);
            //otherwise open it in browser
            Uri uri = Uri.parse(url);
            if (TextUtils.isEmpty(uri.getScheme()))
                url = "http://" + url;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            TEApp.getCurrentActivityContext().startActivity(intent);
            return true;
        }
    }

    private MyWebClient webClient = new MyWebClient();

    public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.message_view, this);
        fullViewHeader = UI.addHeader(R.string.title_activity_message, R.drawable.message, (ViewGroup) getChildAt(0), EnumSet.noneOf(HeaderOptions.class));
        fullViewHeader.setVisibility(View.GONE);
        findViewById(R.id.header_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
        toggleButton = findViewById(R.id.message_view_toggle_button);
        toggleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                makeFullScreen();
            }
        });

        webView = (WebView) findViewById(R.id.message_view_web_view);
        if (isInEditMode() == false) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(webClient);
            webView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    webClient.openInExternalBrowser = true;
                    return false;
                }
            });
        }
        setVisibility(View.GONE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        // prevent touch event from going to main view
        return true;
    }

    public boolean showMessage(final String messageId, String objectId) {
        webClient.openInExternalBrowser = true;
        ITerraExplorerObject messageVar = ISGWorld.getInstance().getCreator().GetObject(messageId);
        ITerraExplorerObject object = null;
        if (TextUtils.isEmpty(objectId) == false) {
            try {
                object = ISGWorld.getInstance().getCreator().GetObject(objectId);
            } catch (ApiException e) {

            }
        }
        boolean hasData = false;
        if (messageVar.getObjectType() == ObjectTypeCode.OT_MESSAGE) {
            ITerraExplorerMessage message = messageVar.CastTo(ITerraExplorerMessage.class);
            if (message.getType() == MsgType.TYPE_SCRIPT && object != null) {
                // we only support script messages of type attribute message
                if (object.getObjectType() == ObjectTypeCode.OT_FEATURE && message.getText().contains("<DOCUMENT_LIST name='Attributes'>") || message.getText().contains("<DOCUMENT_LIST name=\"Attributes\">")) {
                    // open attribute editor
                    final Intent intent = new Intent(TEApp.getCurrentActivityContext(), FeatureAttributesActivity.class);
                    intent.putExtra(FeatureAttributesActivity.FEATURE_ID, object.getID());
                    UI.runOnUiThreadAsync(new Runnable() {
                        @Override
                        public void run() {
                            TEApp.getCurrentActivityContext().startActivity(intent);
                        }
                    });
                }
                return true;
            }
            if (message.getType() == MsgType.TYPE_URL) {
                hasData = loadUrl(message.getURL());
            } else if (message.getType() == MsgType.TYPE_TEXT) {
                hasData = loadText(updateText(message.getText(), object));
            }

        } else if (messageVar.getObjectType() == ObjectTypeCode.OT_POPUP_MESSAGE) {
            IPopupMessage message = messageVar.CastTo(IPopupMessage.class);
            if (TextUtils.isEmpty(message.getSrc()) == false) {
                hasData = loadUrl(message.getSrc());
            } else if (TextUtils.isEmpty(message.getInnerHTML()) == false) {
                hasData = loadHtml(updateText(message.getInnerHTML(), object));
            } else {
                hasData = loadText(updateText(message.getInnerText(), object));
            }
            if (message.getTimeout() > 0) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (messageId.equalsIgnoreCase(displayedMessageId))
                            hide();
                    }
                }, message.getTimeout());
            }
        } else {
            return false;
        }

        if (hasData) {
            UI.runOnUiThreadAsync(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            });
            displayedMessageId = messageId;
        }
        return true;
    }

    private String updateText(String text, ITerraExplorerObject object) {
        if (object == null || object.getObjectType() != ObjectTypeCode.OT_FEATURE)
            return text;
        IFeature feature = object.CastTo(IFeature.class);
        for (int i = 0; i < feature.getFeatureAttributes().getCount(); i++) {
            IFeatureAttribute attribute = ((TEIUnknownHandle) feature.getFeatureAttributes().get_Item(i)).CastTo(IFeatureAttribute.class);
            text = text.replaceAll(String.format("\\$%s\\$", attribute.getName()), attribute.getValue());
        }
        return text;
    }

    private boolean loadUrl(final String url) {
        if (TextUtils.isEmpty(url))
            return false;
        webClient.openInExternalBrowser = false;
        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
        return true;
    }

    private boolean loadHtml(final String html) {
        if (TextUtils.isEmpty(html))
            return false;
        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
            }
        });
        return true;
    }

    private boolean loadText(String text) {
        text = text.replaceAll("\r\n", "<br/>");
        text = text.replaceAll("\n", "<br/>");
        text = text.replaceAll("\r", "<br/>");
        return loadHtml(text);
    }

    private void show() {
        if (getVisibility() == View.VISIBLE)
            return;
        fullViewHeader.setVisibility(View.GONE);
        toggleButton.setVisibility(View.VISIBLE);
        setVisibility(View.VISIBLE);

        getLayoutParams().height = 0;
        View parent = (View) getParent();
        final float targetHeight = parent.getHeight() / 4.0f;
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                getLayoutParams().height = (int) (targetHeight * interpolatedTime);
                requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        a.setDuration(500);
        startAnimation(a);
    }

    public void hide() {
        if (getVisibility() == View.GONE)
            return;
        displayedMessageId = null;
        final int height = getLayoutParams().height;
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                getLayoutParams().height = (int) (height - height * interpolatedTime);
                requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        a.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.GONE);
                webView.loadData(" ", "text/html", null);
            }
        });
        a.setDuration(500);
        startAnimation(a);
    }

    private void makeFullScreen() {
        fullViewHeader.setVisibility(View.VISIBLE);
        fullViewHeader.setAlpha(0);
        toggleButton.setVisibility(View.GONE);
        final int targetHeight = ((View) getParent()).getHeight();
        final int currentHeight = getHeight();
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                getLayoutParams().height = currentHeight + (int) ((targetHeight - currentHeight) * interpolatedTime);
                fullViewHeader.setAlpha(1 * interpolatedTime);
                requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        a.setDuration(500);
        startAnimation(a);
    }

}
