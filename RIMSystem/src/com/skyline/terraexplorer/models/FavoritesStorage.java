package com.skyline.terraexplorer.models;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;

import com.skyline.teapi.AltitudeTypeCode;
import com.skyline.teapi.ApiException;
import com.skyline.teapi.AttributeTypeCode;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.IFeatureGroup;
import com.skyline.teapi.IFeatureLayer;
import com.skyline.teapi.IFeatures;
import com.skyline.teapi.IGeometry;
import com.skyline.teapi.IPoint;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnLoadFinishedListener;
import com.skyline.teapi.ITerraExplorerMessage;
import com.skyline.teapi.LayerGeometryType;
import com.skyline.teapi.MsgTargetPosition;
import com.skyline.teapi.MsgType;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class FavoritesStorage {
    public static final FavoritesStorage defaultStorage = (new FavoritesStorage()).new FavoritesStorageFeatureLayer();

    public static void Init() {
        // causes initialization of defaultStorage
        // which in turn loads favorites layer and subscribes to onloadfinished events
    }

    protected class FavoritesStoragePrefs extends FavoritesStorage {
        private static final String ITEM_SEPARATOR = "yyy";
        private static final String FIELD_SEPARATOR = "xxx";
        @Override
        public FavoriteItem[] getAll() {
            SharedPreferences prefs = TEApp.getAppContext().getSharedPreferences("favorites", Context.MODE_PRIVATE);
            String favoritesString = prefs.getString("favorites", null);
            if (TextUtils.isEmpty(favoritesString))
                return super.getAll();
            String[] parts = favoritesString.split(ITEM_SEPARATOR);
            FavoriteItem[] items = new FavoriteItem[parts.length];
            for (int i = 0; i < parts.length; i++) {
                items[i] = itemFromString(parts[i]);
            }
            return items;
        }

        private FavoriteItem itemFromString(String string) {
            String[] favParts = string.split(FIELD_SEPARATOR);
            FavoriteItem item = new FavoriteItem();
            item.desc = favParts[0];
            item.icon = Integer.parseInt(favParts[1]);
            item.name = favParts[2];
            item.id = favParts[3];
            item.showOn3D = Boolean.parseBoolean(favParts[4]);
            return item;
        }
        private String stringFromItem(FavoriteItem item) {
            return TextUtils.join(FIELD_SEPARATOR, new Object[]{item.desc, item.icon, item.name, item.id, item.showOn3D});
        }
        @Override
        public FavoriteItem getItem(String itemId) {
            for (FavoriteItem item : getAll()) {
                if (item.id.equals(itemId))
                    return item;
            }
            return null;
        }
        @Override
        public void saveItem(FavoriteItem item) {
            deleteItem(item.id);
            String itemString = stringFromItem(item);
            SharedPreferences prefs = TEApp.getAppContext().getSharedPreferences("favorites", Context.MODE_PRIVATE);
            String favoritesString = prefs.getString("favorites", null);
            if (TextUtils.isEmpty(favoritesString))
                favoritesString = itemString;
            else
                favoritesString = favoritesString + ITEM_SEPARATOR + itemString;
            prefs.edit().putString("favorites", favoritesString).apply();
            ;
            Intent intent = new Intent(FavoriteItem.FavoriteChanged.getAction(0));
            intent.putExtra(FavoriteItem.FAVORITE_ID, item.id);
            LocalBroadcastManager.getInstance(TEApp.getAppContext()).sendBroadcast(intent);
        }

        @Override
        public void deleteItem(String itemId) {
            SharedPreferences prefs = TEApp.getAppContext().getSharedPreferences("favorites", Context.MODE_PRIVATE);
            String favoritesString = prefs.getString("favorites", null);
            if (favoritesString == null)
                return;
            String[] parts = favoritesString.split(ITEM_SEPARATOR);
            ArrayList<String> strings = new ArrayList<String>();
            for (int i = 0; i < parts.length; i++) {
                FavoriteItem item = itemFromString(parts[i]);
                if (item.id.equals(itemId) == false)
                    strings.add(parts[i]);
            }
            prefs.edit().putString("favorites", TextUtils.join(ITEM_SEPARATOR, strings)).apply();
        }
    }

    protected class FavoritesStorageFeatureLayer extends FavoritesStorage implements OnLoadFinishedListener {
        private IFeatureLayer favorites;

        public FavoritesStorageFeatureLayer() {
            // prepare favorite images for TE
            int[] iconList = this.iconList();
            for (int i = 0; i < iconList.length; i++) {
                TEImageHelper.prepareImageForTE(iconList[i], Integer.toString(i));
            }

            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    // register for reload/load finished
                    ISGWorld.getInstance().addOnLoadFinishedListener(FavoritesStorageFeatureLayer.this);
                }
            });
        }

        private String getFavoritesPath() {
            String baseDir = Environment.getExternalStorageDirectory() + File.separator + "com.skyline.terraexplorer/files";
            String favoritesPath = baseDir + File.separator + "favorites.shp";
            return favoritesPath;
        }
        @Override
        public void OnLoadFinished(boolean bSuccess) {
            if (bSuccess == false)
                return;

            // check if feature layer of favorites exists in documents directory
            if (new File(getFavoritesPath()).exists() == false) {
                createFavoritesLayerSHP();
            }
            // load favorites layer
            String imagesPath = ISGWorld.getInstance().getApplication().getDataPath();

            imagesPath = imagesPath + "/[icon].png";
            loadFavoritesLayer("TEPlugName=OGR;LayerName=favorites;FileName=" + getFavoritesPath(), imagesPath);
        }

        private void loadFavoritesLayer(String connectionString, String imagesPath) {
            try {
                favorites = ISGWorld.getInstance().getCreator().CreateFeatureLayer("Favorites", connectionString, ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
                favorites.setStreaming(false);
                favorites.getDataSourceInfo().getAttributes().setImportAll(true);

                IFeatureGroup points = favorites.getFeatureGroups().getPoint();
                points.SetProperty("Image file", imagesPath);
                points.SetProperty("Pivot Alignment", 17);
                points.SetProperty("Altitude", "[altitude]");
                points.SetProperty("Altitude Method", 10);
                favorites.getFeatureGroups().SetClassification("EnableShow", "<SQL_EXPR>show_on_3d=1</SQL_EXPR>");
                ITerraExplorerMessage message = ISGWorld.getInstance().getCreator().CreateMessage(MsgTargetPosition.MTP_FLOAT, "$name$<br/>$desc$", MsgType.TYPE_TEXT);

                favorites.getFeatureGroups().SetProperty("Message", message.getID());
                favorites.Load();
            } catch (Exception e) {
                if (new File(getFavoritesPath()).delete())
                    OnLoadFinished(true);
            }
        }

        private void createFavoritesLayerSHP() {
            favorites = ISGWorld.getInstance().getCreator().CreateNewFeatureLayer("Favorites", LayerGeometryType.LGT_POINT, "TEPlugName=OGR;FileName=favorites.shp", ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("name", AttributeTypeCode.AT_TEXT, 200);
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("desc", AttributeTypeCode.AT_TEXT, 2000);
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("show_on_3d", AttributeTypeCode.AT_INTEGER, 1);
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("icon", AttributeTypeCode.AT_TEXT, 40);
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("altitude", AttributeTypeCode.AT_DOUBLE, 10);
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("alt_type", AttributeTypeCode.AT_INTEGER, 10);
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("yaw", AttributeTypeCode.AT_DOUBLE, 10);
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("pitch", AttributeTypeCode.AT_DOUBLE, 10);
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("roll", AttributeTypeCode.AT_DOUBLE, 10);
            favorites.getDataSourceInfo().getAttributes().CreateAttribute("distance", AttributeTypeCode.AT_DOUBLE, 10);
            favorites.Save();
            // remove the layer from TE
            ISGWorld.getInstance().getCreator().DeleteObject(favorites.getID());
            // and copy it to documents dir
            String sourcePath = ISGWorld.getInstance().getApplication().getDataPath() + File.separator + "FeatureLayers/favorites.shp";
            FileUtils.CopyFiles(new File(sourcePath).getParent(), "favorites", new File(getFavoritesPath()).getParent());
        }

        @Override
        public FavoriteItem[] getAll() {
            return UI.runOnRenderThread(new Callable<FavoriteItem[]>() {
                @Override
                public FavoriteItem[] call() throws Exception {
                    IFeatures features = favorites.getFeatureGroups().getPoint().GetCurrentFeatures();
                    int count = features.getCount();
                    FavoriteItem[] result = new FavoriteItem[count];
                    for (int i = 0; i < count; i++) {
                        IFeature feature = ((TEIUnknownHandle) features.get_Item(i)).CastTo(IFeature.class);
                        result[i] = favoriteItemFromFeature(feature);
                    }
                    return result;
                }
            });
        }

        private FavoriteItem favoriteItemFromFeature(IFeature feature) {
            if (feature == null)
                return null;

            try {
                FavoriteItem item = new FavoriteItem();
                item.id = feature.getID();
                item.name = feature.getFeatureAttributes().GetFeatureAttribute("name").getValue();
                item.desc = feature.getFeatureAttributes().GetFeatureAttribute("desc").getValue();
                item.icon = Integer.parseInt(feature.getFeatureAttributes().GetFeatureAttribute("icon").getValue());
                item.showOn3D = feature.getFeatureAttributes().GetFeatureAttribute("show_on_3d").getValue().equalsIgnoreCase("0") ? false : true;
                IPoint point = feature.getGeometry().CastTo(IPoint.class);
                double altitude = Double.parseDouble(feature.getFeatureAttributes().GetFeatureAttribute("altitude").getValue());
                int altitudeType = Integer.parseInt(feature.getFeatureAttributes().GetFeatureAttribute("alt_type").getValue());
                double yaw = Double.parseDouble(feature.getFeatureAttributes().GetFeatureAttribute("yaw").getValue());
                double pitch = Double.parseDouble(feature.getFeatureAttributes().GetFeatureAttribute("pitch").getValue());
                double roll = Double.parseDouble(feature.getFeatureAttributes().GetFeatureAttribute("roll").getValue());
                double distance = Double.parseDouble(feature.getFeatureAttributes().GetFeatureAttribute("distance").getValue());
                item.position = ISGWorld.getInstance().getCreator().CreatePosition(point.getX(), point.getY(), altitude, altitudeType, yaw, pitch, roll, distance);
                return item;
            } catch (NumberFormatException e) {
                // ignore, should not happen anyway
                e.printStackTrace();
            }
            return null;
        }

        private void updateOrCreateFeatureFromFavoriteItem(FavoriteItem item) {
            if (item == null)
                return;


            IPoint geom = ISGWorld.getInstance().getCreator().getGeometryCreator().CreatePointGeometry(new double[]{item.position.getX(), item.position.getY(), 0});

            //////////////////////////////
            //this is a work around for fix bug #18536
            String strWKT = geom.getWks().ExportToWKT();
            ////////////////////////////////

            IFeature feature = null;
            try {
                feature = ISGWorld.getInstance().getCreator().GetObject(item.id).CastTo(IFeature.class);
                feature.setGeometry(geom.CastTo(IGeometry.class));
            } catch (ApiException e) {
                String featureId = favorites.getFeatureGroups().getPoint().CreateFeature(strWKT);
                feature = ISGWorld.getInstance().getCreator().GetObject(featureId).CastTo(IFeature.class);
                item.id = feature.getID();
            }

            item.position.ChangeAltitudeType(AltitudeTypeCode.ATC_TERRAIN_RELATIVE);
            feature.getFeatureAttributes().GetFeatureAttribute("name").setValue(item.name);
            feature.getFeatureAttributes().GetFeatureAttribute("desc").setValue(item.desc);
            feature.getFeatureAttributes().GetFeatureAttribute("icon").setValue(Integer.toString(item.icon));
            feature.getFeatureAttributes().GetFeatureAttribute("show_on_3d").setValue(item.showOn3D ? "1" : "0");
            feature.getFeatureAttributes().GetFeatureAttribute("altitude").setValue(Double.toString(item.position.getAltitude()));
            feature.getFeatureAttributes().GetFeatureAttribute("alt_type").setValue(Integer.toString(item.position.getAltitudeType()));
            feature.getFeatureAttributes().GetFeatureAttribute("yaw").setValue(Double.toString(item.position.getYaw()));
            feature.getFeatureAttributes().GetFeatureAttribute("pitch").setValue(Double.toString(item.position.getPitch()));
            feature.getFeatureAttributes().GetFeatureAttribute("roll").setValue(Double.toString(item.position.getRoll()));
            feature.getFeatureAttributes().GetFeatureAttribute("distance").setValue(Double.toString(item.position.getDistance()));
        }

        @Override
        public void saveItem(final FavoriteItem item) {
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    updateOrCreateFeatureFromFavoriteItem(item);
                    favorites.Save();
                    Intent intent = new Intent(FavoriteItem.FavoriteChanged.getAction(0));
                    intent.putExtra(FavoriteItem.FAVORITE_ID, item.id);
                    LocalBroadcastManager.getInstance(TEApp.getAppContext()).sendBroadcast(intent);
                }
            });
        }

        @Override
        public void deleteItem(final String itemId) {
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    // favorite could have been deleted already
                    // i.e. open query tool, add favorite, opne places, delete favorite, delete favorite from query tool
                    try {
                        ISGWorld.getInstance().getCreator().DeleteObject(itemId);
                    } catch (ApiException e) {
                        // ignore
                    }
                    favorites.Save();
                }
            });
        }

        @Override
        public FavoriteItem getItem(final String itemId) {
            if (itemId == null)
                return null;
            return UI.runOnRenderThread(new Callable<FavoriteItem>() {
                @Override
                public FavoriteItem call() throws Exception {
                    IFeature feature = ISGWorld.getInstance().getCreator().GetObject(itemId).CastTo(IFeature.class);
                    return favoriteItemFromFeature(feature);
                }
            });
        }

    }


    private FavoritesStorage() {

    }

    public FavoriteItem[] getAll() {


        return new FavoriteItem[]{};
    }

    public int[] iconList() {
        return new int[]{R.drawable.star_red, R.drawable.star_green, R.drawable.star_purple, R.drawable.star_blue, R.drawable.star_yellow};
    }

    public int resourceForIcon(int icon) {
        if (icon > 4)
            icon = 0;
        return iconList()[icon];
    }

    public String descriptionForIcon(int icon) {
        if (icon > 4)
            icon = 0;
        switch (icon) {
            case 0:
                return TEApp.getAppContext().getString(R.string.favorite_icon_star_red);
            case 1:
                return TEApp.getAppContext().getString(R.string.favorite_icon_star_green);
            case 2:
                return TEApp.getAppContext().getString(R.string.favorite_icon_star_purple);
            case 3:
                return TEApp.getAppContext().getString(R.string.favorite_icon_star_blue);
            case 4:
                return TEApp.getAppContext().getString(R.string.favorite_icon_star_yellow);
        }
        return "";
    }

    public void saveItem(FavoriteItem item) {

    }

    public void deleteItem(String itemId) {

    }

    public FavoriteItem getItem(String itemId) {
        return null;
    }

}