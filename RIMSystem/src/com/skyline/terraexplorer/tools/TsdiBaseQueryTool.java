package com.skyline.terraexplorer.tools;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.skyline.teapi.ApiException;
import com.skyline.teapi.ContourDisplayStyle;
import com.skyline.teapi.CoverageArea;
import com.skyline.teapi.IContourMap;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISlopeMap;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.ITerrainImageLabel;
import com.skyline.teapi.IWorldPointInfo;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.teapi.SlopeDisplayStyle;
import com.skyline.teapi.WorldPointType;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.TEAppException;
import com.skyline.terraexplorer.controllers.FeatureAttributesActivity;
import com.skyline.terraexplorer.models.FavoriteItem;
import com.skyline.terraexplorer.models.FavoritesStorage;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.SearchWeb;
import com.skyline.terraexplorer.models.TEImageHelper;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.TEView.OnLongPressListener;
import com.skyline.terraexplorer.views.ToolContainer;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_OBJECT_ATTRIBUTE;

/**
 * Created by mft on 2017/8/21.
 */

public class TsdiBaseQueryTool extends TsdiBaseTool implements ISGWorld.OnLoadFinishedListener, OnLongPressListener {
    private class ReverseGeoCodeAsyncTask extends AsyncTask<PointF, Void, ArrayList<SearchWeb.SearchResult>> {
        private TEAppException error;
        private PointF point;

        @Override
        protected ArrayList<SearchWeb.SearchResult> doInBackground(PointF... points) {
            SearchWeb searcher = new SearchWeb();
            try {
                point = points[0];
                return searcher.reverseGeoCode(point);
            } catch (TEAppException ex) {
                error = ex;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<SearchWeb.SearchResult> result) {

            String name = "";
            if (error != null) {
                name = error.getLocalizedMessage();
            } else {
                searchResult = null;
                if (result != null && result.isEmpty() == false)
                    searchResult = result.get(0);
                if (searchResult == null || TextUtils.isEmpty(searchResult.name)) {
                    return;
                }
                name = String.format("%s, %s", searchResult.name, searchResult.desc);
            }
//            if(locationText.length()>0 && toolContainer.isUpperViewHidden())
//                toolContainer.setUpperViewHidden(false);
//            else if(locationText.length()==0 && toolContainer.isUpperViewHidden()==false)
//                toolContainer.setUpperViewHidden(true);
            toolContainer.setText(String.format("%s\r\n%s", name, locationText));
        }
    }

    private IContourMap contourMap;
    private ISlopeMap slopeMap;
    private ITerrainImageLabel imageLabel;
    private ImageView imageView;
    private SearchWeb.SearchResult searchResult;
    private String favoriteItemId;
    private String locationText;
    private String queryImagePath;
    private String queryFavoritImagePath;
    private ReverseGeoCodeAsyncTask geocodeTask;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            favoriteChanged(intent.getExtras().getString(FavoriteItem.FAVORITE_ID));
        }
    };
    private IFeature feature;

    public TsdiBaseQueryTool() {
        initQueryImage();
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().addOnLoadFinishedListener(TsdiBaseQueryTool.this);
            }
        });
        UI.getTEView().addOnLongPressListener(this);
    }

    private void initQueryImage() {
        queryImagePath = TEImageHelper.prepareImageForTE(R.drawable.query);
        queryFavoritImagePath = TEImageHelper.prepareImageForTE(R.drawable.favorit_places_yellow);
    }

    @Override
    public boolean onBeforeCloseToolContainer(ToolContainer.CloseReason closeReason) {
        super.onBeforeCloseToolContainer(closeReason);
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                //ISGWorld.getInstance().removeOnAnalysisDistancePointAddedListener(DistanceTool.this);
            }
        });
        if (imageLabel != null) {
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    ISGWorld.getInstance().getCreator().DeleteObject(imageLabel.getID());
                }
            });
            imageLabel = null;
        }
        if (imageView != null) {
            ((ViewGroup) imageView.getParent()).removeView(imageView);
            imageView = null;
        }
        if (geocodeTask != null) {
            geocodeTask.cancel(true);
            geocodeTask = null;
        }
        searchResult = null;
        favoriteItemId = null;

        if (feature != null) {
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    feature.getTint().SetAlpha(0);
                }
            });
            feature = null;
        }
        // remove subscription
        LocalBroadcastManager.getInstance(TEApp.getAppContext()).unregisterReceiver(broadcastReceiver);
        return true;
    }

    /***
     * 信息查询
     * @param param 查询点位置
     */
    public void query(Object param) {
        final Point coords = (Point) param;
        final IWorldPointInfo pointInfo =
                UI.runOnRenderThread(new Callable<IWorldPointInfo>() {

                    @Override
                    public IWorldPointInfo call() throws Exception {
                        IWorldPointInfo pointInfo = ISGWorld.getInstance().getWindow().PixelToWorld(coords.x, coords.y);
                        if ((pointInfo.getType() & WorldPointType.WPT_SKY) != 0 || (pointInfo.getType() & WorldPointType.WPT_SCREEN_CONTROL) != 0)
                            return null;
                        locationText = String.format("[%.2f %.2f]", pointInfo.getPosition().getX(), pointInfo.getPosition().getY());
                        return pointInfo;
                    }
                });
        if (pointInfo == null)
            return;
        if (ToolContainer.INSTANCE.showWithDelegate(this) == false) {
            return;
        }
        // check if the long press was on feature. If it was, we need add feature attributes button.
        feature = UI.runOnRenderThread(new Callable<IFeature>() {
            @Override
            public IFeature call() throws Exception {
                if (TextUtils.isEmpty(pointInfo.getObjectID()))
                    return null;
                ITerraExplorerObject obj = null;
                try {
                    obj = ISGWorld.getInstance().getCreator().GetObject(pointInfo.getObjectID());
                } catch (ApiException e) {
                    // ignore the error
                    // user may have clicked on the favorite icon itself and it does not exists anymore.
                    // anyway, it is better to ignore the clicked object than to crash.
                }
                if (obj == null || obj.getObjectType() != ObjectTypeCode.OT_FEATURE)
                    return null;
                IFeature feature = obj.CastTo(IFeature.class);
                String parentId = feature.getParentGroupID();
                obj = ISGWorld.getInstance().getCreator().GetObject(parentId);
                // feature.setTint is not implemented for 3dml features
                if (obj.getObjectType() != ObjectTypeCode.OT_3D_MESH_FEATURE_LAYER)
                    feature.setTint(ISGWorld.getInstance().getCreator().CreateColor(255, 0, 0));
                return feature;
            }
        });
        if (feature != null)
            toolContainer.addButton(TE_FUNCTION_BUTTON_OBJECT_ATTRIBUTE, R.drawable.list);

        // add image label to te and hide it
        imageLabel = UI.runOnRenderThread(new Callable<ITerrainImageLabel>() {

            @Override
            public ITerrainImageLabel call() throws Exception {
                ITerrainImageLabel imageLabel = ISGWorld.getInstance().getCreator().CreateImageLabel(pointInfo.getPosition(), queryImagePath);
                ISGWorld.getInstance().getProjectTree().SetParent(imageLabel.getID(), ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
                ISGWorld.getInstance().getProjectTree().SetVisibility(imageLabel.getID(), false);
                return imageLabel;
            }
        });
        final String labelId = UI.runOnRenderThread(new Callable<String>() {

            @Override
            public String call() throws Exception {
                return imageLabel.getID();
            }
        });

        //add label and animate it
        imageView = new ImageView(TEApp.getAppContext());
        imageView.setImageResource(R.drawable.query);
        imageView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        UI.getMainView().addView(imageView);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) (imageView.getLayoutParams());
        lp.leftMargin = coords.x - (int) (0.5 * imageView.getMeasuredWidth());
        lp.topMargin = coords.y - (int) (1.5 * imageView.getMeasuredHeight());
        imageView.requestLayout();
        imageView.setPivotX(imageView.getMeasuredWidth() / 2.0f);
        imageView.setPivotY(imageView.getMeasuredHeight());
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(imageView, View.SCALE_X, 1, 2, 1),
                ObjectAnimator.ofFloat(imageView, View.SCALE_Y, 1, 2, 1)
        );
        set.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                UI.runOnRenderThreadAsync(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("tsdi", "onAnimationEnd");
                        // imageView can be null if tool was closed before animation finished.
                        // i.e if presentation playing and firing events every second
                        // also return if user clicked on another location: detected by new imageLabel
                        if (imageView == null || (imageLabel != null && labelId.equals(imageLabel.getID()) == false))
                            return;
                        Log.i("tsdi", "onAnimationEnd  111");
                        // show TE label after animation ended
                        UI.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // remove imageView after animation ended
                                Log.i("tsdi", "onAnimationEnd  222");
                                ((ViewGroup) imageView.getParent()).removeView(imageView);
                                imageView = null;
                            }
                        });
                        ISGWorld.getInstance().getProjectTree().SetVisibility(imageLabel.getID(), true);
                    }
                });
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        set.setDuration(200).start();
        geocodeTask = new ReverseGeoCodeAsyncTask();
        PointF point = UI.runOnRenderThread(new Callable<PointF>() {

            @Override
            public PointF call() throws Exception {
                return new PointF((float) imageLabel.getPosition().getX(), (float) imageLabel.getPosition().getY());
            }
        });
        geocodeTask.execute(point);
    }

    @Override
    public void onLongPress(MotionEvent ev) {
        if (ToolContainer.INSTANCE.isVisible() && toolContainer == null) {
            // tool container is visible, but not for this tool
            // do not open the tool
            return;
        }
        Point coords = new Point((int) ev.getX(), (int) ev.getY());
        query(coords);
    }

    @Override
    public void OnLoadFinished(boolean bSuccess) {
        if (bSuccess == false)
            return;
        contourMap = ISGWorld.getInstance().getAnalysis().CreateContourMap(0, 0, 0, 0, ContourDisplayStyle.CDS_CONTOUR_STYLE_LINES_AND_COLORS, "f60a5b60-6e39-11e0-ae3e-0800200c9a66", ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
        contourMap.setCoverageArea(CoverageArea.CA_ENTIRE_TERRAIN);
        contourMap.setContourLinesInterval(0);
        ISGWorld.getInstance().getProjectTree().SetVisibility(contourMap.getID(), false);

        slopeMap = ISGWorld.getInstance().getAnalysis().CreateSlopeMap(-180, 90, 180, -90, SlopeDisplayStyle.SDS_SLOPE_STYLE_DIRECTION_AND_COLORS, "A3BB1673-D8F4-4455-9529-A303A034FCEB", ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
        slopeMap.setCoverageArea(CoverageArea.CA_ENTIRE_TERRAIN);
        ISGWorld.getInstance().getProjectTree().SetVisibility(slopeMap.getID(), false);
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        super.onBeforeOpenToolContainer();
        toolContainer.setText(locationText);
        // subscribe to notification favorite changed
        LocalBroadcastManager.getInstance(TEApp.getAppContext()).registerReceiver(broadcastReceiver, FavoriteItem.FavoriteChanged);
//        if(locationText==null || locationText.length()==0)
//            toolContainer.setUpperViewHidden(true);
        return true;
    }

    private void favoriteChanged(String changedFavoriteId) {
        // if showOn3D for current fav item was changed , update visibility of query label
        if (favoriteItemId != null && favoriteItemId.equals(changedFavoriteId)) {
            final FavoriteItem favItem = FavoritesStorage.defaultStorage.getItem(changedFavoriteId);
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    ISGWorld.getInstance().getProjectTree().SetVisibility(imageLabel.getID(), !favItem.showOn3D);
                }
            });
        }
    }

    @Override
    protected void showNormalButtons() {
        super.showNormalButtons();
    }

    @Override
    public void onButtonClick(int tag) {
        super.onButtonClick(tag);
        switch (tag) {
            case TE_FUNCTION_BUTTON_OBJECT_ATTRIBUTE: //查看feature属性
                final String featureId = UI.runOnRenderThread(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        // TODO Auto-generated method stub
                        return feature.getID();
                    }
                });
                // open attribute editor
                Intent intent = new Intent(TEApp.getCurrentActivityContext(), FeatureAttributesActivity.class);
                intent.putExtra(FeatureAttributesActivity.FEATURE_ID, featureId);
                TEApp.getCurrentActivityContext().startActivity(intent);
                break;
            default:
                break;
        }
    }
}
