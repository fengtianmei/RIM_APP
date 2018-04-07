package com.skyline.terraexplorer.models;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.skyline.teapi.ApiException;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.TEMainActivity;
import com.skyline.terraexplorer.tools.SearchTool;
import com.skyline.terraexplorer.views.TEGLRenderer;
import com.skyline.terraexplorer.views.TEView;

import java.util.EnumSet;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public class UI {
    private static final int HEADER_TAG = 345334535;

    public static float scaleFactor() {
        return TEApp.getAppContext().getResources().getDimension(R.dimen.scale_factor);
    }

    public static float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, TEApp.getAppContext().getResources().getDisplayMetrics());
    }

    public static int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, TEApp.getAppContext().getResources().getDisplayMetrics());
    }

    public enum HeaderOptions {
        SearchButton,
        NoBackButton
    }

    private static View addHeaderImpl(int name, int image, ViewGroup parentView) {
        LayoutInflater inflater = LayoutInflater.from(parentView.getContext());
        View header = inflater.inflate(R.layout.header, parentView, false);
        ((ImageView) header.findViewById(R.id.header_icon)).setImageResource(image);
        ((TextView) header.findViewById(R.id.header_title)).setText(name);
        parentView.addView(header, 0);
        parentView.setBackgroundColor(parentView.getContext().getResources().getColor(R.color.color_view_background));
        header.setTag(HEADER_TAG);
        return header;
    }

    public static View addHeader(int name, int image, Activity activity) {
        return addHeader(name, image, activity, EnumSet.noneOf(HeaderOptions.class));
    }

    public static View addHeader(int name, int image, final Activity activity, EnumSet<HeaderOptions> options) {
        View header = addHeader(name, image, getContentView(activity), options);
        header.findViewById(R.id.header_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.finish();
            }
        });
        return header;
    }

    public static View addHeader(int name, int image, final ViewGroup view, EnumSet<HeaderOptions> options) {
        View header = addHeaderImpl(name, image, view);

        header.findViewById(R.id.header_search).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ToolManager.INSTANCE.openTool(SearchTool.class.getName());
            }
        });

        if (options.contains(HeaderOptions.NoBackButton))
            header.findViewById(R.id.header_back).setVisibility(View.GONE);
        else
            header.findViewById(R.id.header_back).setVisibility(View.VISIBLE);

        if (options.contains(HeaderOptions.SearchButton))
            header.findViewById(R.id.header_search).setVisibility(View.VISIBLE);
        else
            header.findViewById(R.id.header_search).setVisibility(View.GONE);

        return header;
    }

    public static TEView getTEView() {
        return TEView.getInstance();
    }

    public static RelativeLayout getMainView() {
        return (RelativeLayout) getTEView().getParent();
    }

    public static ExpandableListView addFullScreenTable(Activity activity) {
        LayoutInflater.from(activity).inflate(R.layout.table, getContentView(activity));

        return (ExpandableListView) activity.findViewById(android.R.id.list);
    }

    private static ViewGroup getContentView(Activity activity) {
        ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
        ViewGroup child = (ViewGroup) rootView.getChildAt(0);
        if (child == null) {
            LinearLayout ll = new LinearLayout(activity);
            ll.setOrientation(LinearLayout.VERTICAL);
            activity.setContentView(ll);
            child = ll;
        }
        return child;
    }

    public static void runOnUiThreadAsync(Runnable action) {
        Activity activity = (Activity) TEApp.getCurrentActivityContext();
        activity.runOnUiThread(action);
    }


    public static void runOnRenderThreadAsync(Runnable action) {
        if (Thread.currentThread().getId() != TEGLRenderer._ThreadID) {
            getTEView().queueEvent(action);
        } else {
            action.run();
        }


    }

    public static void runOnRenderThread(Runnable action) {
        runAndWait(action, new Executor() {
            @Override
            public void execute(Runnable command) {
                runOnRenderThreadAsync(command);
            }
        });
    }

    public static void runOnUiThread(Runnable action) {
        runAndWait(action, new Executor() {
            @Override
            public void execute(Runnable command) {
                runOnUiThreadAsync(command);
            }
        });
    }

    private static void runAndWait(final Runnable action, Executor executor) {
        final CountDownLatch latch = new CountDownLatch(1);
        final ValueHolder<ApiException> errorHolder = new ValueHolder<ApiException>();

        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } catch (ApiException ex) {
                    errorHolder.value = ex;
                }
                latch.countDown();
            }
        };
        executor.execute(task);
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException e1) {
                // ignore and continue waiting
            }
        }
        if (errorHolder.value != null)
            throw errorHolder.value;
    }

    private static class ValueHolder<T> {
        T value;
    }

    public static <T> T runOnRenderThread(Callable<T> action) {
        return runOnThread(action, new Executor() {
            @Override
            public void execute(Runnable command) {
                runOnRenderThread(command);
            }
        });
    }

    public static <T> T runOnUiThread(Callable<T> action) {
        return runOnThread(action, new Executor() {
            @Override
            public void execute(Runnable command) {
                runOnUiThread(command);
            }
        });
    }

    private static <T> T runOnThread(final Callable<T> action, Executor executor) {
        final ValueHolder<T> valueHolder = new ValueHolder<T>();
        final ValueHolder<ApiException> errorHolder = new ValueHolder<ApiException>();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    valueHolder.value = action.call();
                } catch (Exception e) {
                    if (e instanceof ApiException)
                        errorHolder.value = (ApiException) e;
                    else
                        errorHolder.value = new ApiException(e);
                }
            }
        };
        executor.execute(task);
        if (errorHolder.value != null)
            throw errorHolder.value;
        return valueHolder.value;
    }

    public static int randomColor() {
        Random rand = new Random();
        float hue = rand.nextFloat() * 360; // 0.0 - 360
        float saturation = rand.nextFloat() * 0.5f + 0.5f; // 0.5 to 1.0, away from white
        float brightness = rand.nextFloat() * 0.5f + 0.5f; // 0.5 to 1.0, away from white
        return Color.HSVToColor(new float[]{hue, saturation, brightness});
    }

    public static void popToMainActivity() {
        Intent intent = new Intent(TEApp.getCurrentActivityContext(), TEMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Removes other Activities from stack
        TEApp.getCurrentActivityContext().startActivity(intent);
    }
}
