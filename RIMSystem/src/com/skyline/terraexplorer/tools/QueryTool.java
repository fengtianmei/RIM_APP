package com.skyline.terraexplorer.tools;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
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
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;

import com.skyline.teapi.ActionCode;
import com.skyline.teapi.ApiException;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.IPosition;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.ITerrainImageLabel;
import com.skyline.teapi.IWorldPointInfo;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.teapi.WorldPointType;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.TEAppException;
import com.skyline.terraexplorer.controllers.FeatureAttributesActivity;
import com.skyline.terraexplorer.models.FavoriteItem;
import com.skyline.terraexplorer.models.FavoritesStorage;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.SearchWeb;
import com.skyline.terraexplorer.models.SearchWeb.SearchResult;
import com.skyline.terraexplorer.models.TEImageHelper;
import com.skyline.terraexplorer.models.ToolManager;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.TEView.OnLongPressListener;
import com.skyline.terraexplorer.views.ToolContainer;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class QueryTool extends BaseToolWithContainer implements OnLongPressListener {

    private class ReverseGeoCodeAsyncTask extends AsyncTask<PointF, Void, ArrayList<SearchResult>> {
        private TEAppException error;
        private PointF point;

        @Override
        protected ArrayList<SearchResult> doInBackground(PointF... points) {
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
        protected void onPostExecute(ArrayList<SearchResult> result) {
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
            toolContainer.setText(String.format("%s\r\n%s", name, locationText));
        }
    }

    private ITerrainImageLabel imageLabel;
    private ImageView imageView;
    private SearchResult searchResult;
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

    public QueryTool() {
        initQueryImage();
        // force loading of favorites
        FavoritesStorage.Init();
        UI.getTEView().addOnLongPressListener(this);
    }

    private void initQueryImage() {
        queryImagePath = TEImageHelper.prepareImageForTE(R.drawable.query);
        queryFavoritImagePath = TEImageHelper.prepareImageForTE(R.drawable.favorit_places_yellow);
    }

    @Override
    public void open(Object param) {
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
            toolContainer.addButton(4, R.drawable.list);

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
        imageView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        UI.getMainView().addView(imageView);
        MarginLayoutParams lp = (MarginLayoutParams) (imageView.getLayoutParams());
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
        set.addListener(new AnimatorListener() {

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
                        // imageView can be null if tool was closed before animation finished.
                        // i.e if presentation playing and firing events every second
                        // also return if user clicked on another location: detected by new imageLabel
                        if (imageView == null || (imageLabel != null && labelId.equals(imageLabel.getID()) == false))
                            return;
                        // show TE label after animation ended
                        UI.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // remove imageView after animation ended
                                ((ViewGroup) imageView.getParent()).removeView(imageView);
                                imageView = null;
                                Log.i("tsdi", "QueryTool777777 ");
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
    public boolean onBeforeOpenToolContainer() {
        Log.i("tsdi", "QueryTool 77");
        toolContainer.setText(locationText);
        toolContainer.addButton(1, R.drawable.favorits_checkbox);
        toolContainer.addButton(3, R.drawable.fly_around);
        Log.i("tsdi", "QueryTool 88");
        // subscribe to notification favorite changed
        LocalBroadcastManager.getInstance(TEApp.getAppContext()).registerReceiver(broadcastReceiver, FavoriteItem.FavoriteChanged);
        return true;
    }

    @Override
    public void onButtonClick(int tag) {
        switch (tag) {
            case 1: // add favorite
            {
                addFavorite();
                break;
            }
            case 2: // remove favorite
            {
                removeFavorite();
                break;
            }
            case 3: // fly around
            {
                UI.runOnRenderThread(new Runnable() {
                    @Override
                    public void run() {
                        ISGWorld.getInstance().getNavigate().FlyTo(imageLabel.getID(), ActionCode.AC_CIRCLEPATTERN);
                    }
                });
                break;
            }
            case 4: {
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
            }
            default:
                break;
        }
    }

    @Override
    public void onLongPress(MotionEvent ev) {

        if (ToolContainer.INSTANCE.isVisible() && toolContainer == null) {
            // tool container is visible, but not for this tool
            // do not open the tool
            return;
        }
        Log.i("tsdi", "begin open()");
        Point coords = new Point((int) ev.getX(), (int) ev.getY());
        open(coords);
    }

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
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

    private void addFavorite() {
        // create fav item
        FavoriteItem favItem = new FavoriteItem();
        if (searchResult != null && TextUtils.isEmpty(searchResult.name) == false) {
            favItem.name = searchResult.name;
            favItem.desc = searchResult.desc;
        } else {
            favItem.name = locationText;
        }

        IPosition pos = UI.runOnRenderThread(new Callable<IPosition>() {
            @Override
            public IPosition call() throws Exception {
                imageLabel.setImageFileName(queryFavoritImagePath);
                return ISGWorld.getInstance().getCreator().CreatePosition(imageLabel.getPosition().getX(), imageLabel.getPosition().getY(), imageLabel.getPosition().getAltitude(), imageLabel.getPosition().getAltitudeType(), 0, -75, 0, 5000);
            }
        });

        favItem.position = pos;
        // save it
        FavoritesStorage.defaultStorage.saveItem(favItem);
        // remember the id
        favoriteItemId = favItem.id;
        // update buttons
        toolContainer.removeButtons();
        toolContainer.addButton(2, R.drawable.favorits);
        toolContainer.addButton(3, R.drawable.fly_around);

        // open edit tool
        ToolManager.INSTANCE.openTool(EditFavoriteTool.class.getName(), favItem.id);
    }

    private void removeFavorite() {
        // delete from favorites
        FavoritesStorage.defaultStorage.deleteItem(favoriteItemId);
        // update buttons
        toolContainer.removeButtons();
        toolContainer.addButton(1, R.drawable.favorits_checkbox);
        toolContainer.addButton(3, R.drawable.fly_around);
        // restore visibility that might have changed because of showOn3D
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                imageLabel.setImageFileName(queryImagePath);
                ISGWorld.getInstance().getProjectTree().SetVisibility(imageLabel.getID(), true);
            }
        });
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
}
