package com.skyline.terraexplorer.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.skyline.teapi.AltitudeTypeCode;
import com.skyline.teapi.AttributeTypeCode;
import com.skyline.teapi.GPSOperationMode;
import com.skyline.teapi.IFeatureGroup;
import com.skyline.teapi.IFeatureLayer;
import com.skyline.teapi.IGeometry;
import com.skyline.teapi.ILineString;
import com.skyline.teapi.IPoint;
import com.skyline.teapi.IPosition;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnLoadFinishedListener;
import com.skyline.teapi.ISGWorld.OnSGWorldListener;
import com.skyline.teapi.ITerrainPolyline;
import com.skyline.teapi.LayerGeometryType;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.FileUtils;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.LocationTracker;
import com.skyline.terraexplorer.models.LocationTracker.LocationTrackerDelegate;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.UI;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class GpsTool extends BaseTool implements LocationTrackerDelegate, OnLoadFinishedListener, OnSGWorldListener {
    private boolean locationServicesOn = false;
    private LocationTracker locationTracker = new LocationTracker();
    private ImageButton locationLockButton;

    private String gpsTrailDisplayGroup;
    private Location gpsTrailLastAddedLocation;
    private Location gpsTrailLastUsedLocation;
    private ArrayList<String> gpsTrailSegments;
    private ITerrainPolyline gpsTrailLastAddedLine;
    private IFeatureLayer gpsTrailFeatureLayer;
    private boolean gpsTrailFeatureLayerSaveQueued;

    private boolean cameraLocked = false;
    private boolean showUserPosition = false;
    private boolean showGpsTrail = false;

    public GpsTool() {
        locationTracker.setDelegate(this);
        locationLockButton = new ImageButton(TEApp.getAppContext());
        locationLockButton.setImageResource(R.drawable.gps);
        int padding = (int) locationLockButton.getResources().getDimension(R.dimen.button_padding);
        locationLockButton.setPadding(padding, padding, padding, padding);
        locationLockButton.setBackgroundColor(locationLockButton.getResources().getColor(R.color.color_control_background));
        locationLockButton.setVisibility(View.GONE);
        UI.getMainView().addView(locationLockButton);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) locationLockButton.getLayoutParams();
        lp.topMargin = (int) (20 * UI.scaleFactor());
        lp.rightMargin = (int) (20 * UI.scaleFactor());
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, -1);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, -1);

        locationLockButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                restoreLocationLock();
            }
        });

        LocalBroadcastManager.getInstance(TEApp.getAppContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                settingChanged(intent);
            }
        }, SettingsTool.SettingChanged);

        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().addOnLoadFinishedListener(GpsTool.this);
                ISGWorld.getInstance().addOnSGWorldListener(GpsTool.this);
            }
        });

    }

    @Override
    public MenuEntry getMenuEntry() {
        if (locationServicesOn)
            return MenuEntry.createFor(this, R.string.mm_gps_off, R.drawable.gps, 50);
        else
            return MenuEntry.createFor(this, R.string.mm_gps_on, R.drawable.gps, 50);
    }

    @Override
    public void open(Object param) {
        if (locationTracker.isGpsAvaliable(true) == false)
            return;
        locationServicesOn = !locationServicesOn;
        setCameraLocked(locationServicesOn);
        setShowGpsTrail(locationServicesOn &&
                TEApp.getAppContext()
                        .getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE)
                        .getBoolean(TEApp.getAppContext().getString(R.string.key_gps_trail), true));
        setShowUserPosition(locationServicesOn &&
                TEApp.getAppContext()
                        .getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE)
                        .getBoolean(TEApp.getAppContext().getString(R.string.key_my_position), true));
        updateTrackerState();
    }

    private void setShowUserPosition(boolean showUserPosition) {
        this.showUserPosition = showUserPosition;
    }

    public void OnLoadFinished(boolean bSuccess) {
        if (bSuccess == false)
            return;
        gpsTrailDisplayGroup = null;
        gpsTrailLastAddedLocation = null;
        setShowGpsTrail(false);
        setShowUserPosition(false);
        setCameraLocked(false);
        locationServicesOn = false;
        updateTrackerState();
    }

    public void OnSGWorld(int param, Object object) {
        if (param == 11 /*TEE_GPS_LOCK_BREAK*/ && cameraLocked == true) {
            setCameraLocked(false);
            updateTrackerState();
        }
    }

    private void restoreLocationLock() {
        setCameraLocked(true);
        updateTrackerState();
    }


    private void setCameraLocked(final boolean cameraLocked) {
        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                locationLockButton.setVisibility((cameraLocked || locationServicesOn == false) ? View.GONE : View.VISIBLE);
            }
        });
        this.cameraLocked = cameraLocked;
    }

    int calculateGpsMode() {
        if (locationServicesOn == false)
            return GPSOperationMode.GPS_MODE_OFF;

        int gpsMode = GPSOperationMode.GPS_MODE_OFF;
        if (showUserPosition)
            gpsMode |= GPSOperationMode.GPS_MODE_SHOW_LOCATION_INDICATOR;
        if (cameraLocked)
            gpsMode |= GPSOperationMode.GPS_MODE_FOLLOW;

        return gpsMode;
    }

    private void setShowGpsTrail(boolean showGpsTrail) {
        if (this.showGpsTrail == showGpsTrail)
            return;
        this.showGpsTrail = showGpsTrail;

        if (showGpsTrail == false && gpsTrailDisplayGroup != null) {
            UI.runOnRenderThreadAsync(new Runnable() {
                @Override
                public void run() {
                    ISGWorld.getInstance().getProjectTree().DeleteItem(gpsTrailDisplayGroup);
                }
            });
            gpsTrailLastAddedLocation = null;
            gpsTrailDisplayGroup = null;
            gpsTrailSegments = null;
            gpsTrailLastAddedLine = null;
        }

        if (gpsTrailSegments == null)
            gpsTrailSegments = new ArrayList<String>();
    }

    private void updateTrackerState() {
        final int gpsMode = calculateGpsMode();
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().getNavigate().SetGPSMode(gpsMode);
            }
        });
        if (locationServicesOn) {
            Location lastKnownLocation = locationTracker.getLastKnownLocation();
            if (lastKnownLocation != null)
                locationUpdate(lastKnownLocation);
            UI.runOnUiThreadAsync(new Runnable() {
                @Override
                public void run() {
                    locationTracker.connect();
                }
            });
        } else {
            UI.runOnUiThreadAsync(new Runnable() {
                @Override
                public void run() {
                    locationTracker.disconnect();
                }
            });
        }

    }

    @Override
    public void locationTrackerStateChanged(boolean connected) {
        if (connected == false && locationServicesOn)
            startGPSIconBlinking();
    }


    private void startGPSIconBlinking() {
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                // if after two seconds camera still locked and we do not have a connection
                if (locationServicesOn && locationTracker.isConnected() == false) {
                    Toast.makeText(TEApp.getAppContext(), TEApp.getAppContext().getString(R.string.gpstool_nogps), Toast.LENGTH_SHORT).show();
                }

            }
        }, 2000);
    }

    @Override
    public void locationUpdate(final Location location) {
        if (showGpsTrail) {
            UI.runOnRenderThreadAsync(new Runnable() {
                @Override
                public void run() {
                    updateGpsTrail(location);
                }
            });
        }

        if (calculateGpsMode() != GPSOperationMode.GPS_MODE_OFF) {
            UI.runOnRenderThreadAsync(new Runnable() {
                @Override
                public void run() {
                    IPosition pos1 = location2position(location);
                    ISGWorld.getInstance().getNavigate().SetGPSPosition(pos1);
                }
            });
        }
    }

    private void updateGpsTrail(Location location) {
        if (gpsTrailDisplayGroup == null)
            gpsTrailDisplayGroup = ISGWorld.getInstance().getProjectTree().CreateGroup("GPS Trail", ISGWorld.getInstance().getProjectTree().getHiddenGroupID());

        if (gpsTrailLastAddedLocation == null) {
            gpsTrailLastAddedLocation = location;
            gpsTrailLastUsedLocation = location;
        }

        // add new segment to the trail once per 4 seconds
        if (gpsTrailLastAddedLocation.getTime() - (new Date().getTime()) <= -4000 || gpsTrailSegments.size() == 0) {
            ILineString geom = ISGWorld.getInstance().getCreator().getGeometryCreator().CreateLineStringGeometry(new double[]{
                    gpsTrailLastUsedLocation.getLongitude(), gpsTrailLastUsedLocation.getLatitude(), 0,
                    location.getLongitude(), location.getLatitude(), 0,
            });

            gpsTrailLastAddedLine = ISGWorld.getInstance().getCreator().CreatePolyline(geom.CastTo(IGeometry.class), "#ff8040", AltitudeTypeCode.ATC_ON_TERRAIN, gpsTrailDisplayGroup);
            gpsTrailLastAddedLine.getLineStyle().setWidth(-2);
            gpsTrailLastAddedLine.getLineStyle().setPattern(0xC3C3C3C3);
            gpsTrailSegments.add(gpsTrailLastAddedLine.getID());
            gpsTrailLastAddedLocation = gpsTrailLastUsedLocation;
        } else // otherwise just move last point in last segment to current location
        {
            ILineString geom = ISGWorld.getInstance().getCreator().getGeometryCreator().CreateLineStringGeometry(new double[]{
                    gpsTrailLastAddedLocation.getLongitude(), gpsTrailLastAddedLocation.getLatitude(), 0,
                    location.getLongitude(), location.getLatitude(), 0,
            });
            gpsTrailLastAddedLine.setGeometry(geom.CastTo(IGeometry.class));
        }
        gpsTrailLastUsedLocation = location;

        // limit gps trail to 100 segments
        while (gpsTrailSegments.size() > 100) {
            ISGWorld.getInstance().getCreator().DeleteObject(gpsTrailSegments.get(0));
            gpsTrailSegments.remove(0);
        }

        // add point to gps points layer
        if (gpsTrailFeatureLayer == null) {
            // if gps points file does not exists, create it and copy to documents
            String gpsPointsFile = Environment.getExternalStorageDirectory() + "/com.skyline.terraexplorer/files/gps_points.shp";
            if ((new File(gpsPointsFile)).exists() == false) {
                String connectionString = "TEPlugName=OGR;FileName=gps_points.shp";
                IFeatureLayer layer = ISGWorld.getInstance().getCreator().CreateNewFeatureLayer("gps_points", LayerGeometryType.LGT_POINT, connectionString, ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
                layer.getDataSourceInfo().getAttributes().CreateAttribute("timestamp", AttributeTypeCode.AT_DOUBLE, 200);
                layer.Save();
                // remove the layer from TE
                ISGWorld.getInstance().getCreator().DeleteObject(layer.getID());
                String sourcePath = ISGWorld.getInstance().getApplication().getDataPath() + File.separator + "FeatureLayers/gps_points.shp";
                String fileName = new File(sourcePath).getName();
                FileUtils.CopyFiles(new File(sourcePath).getParent(), fileName.substring(0, fileName.length() - ".shp".length()), new File(gpsPointsFile).getParent());

            }

            // and now load the layer
            gpsTrailFeatureLayer = ISGWorld.getInstance().getCreator().CreateFeatureLayer("GPS Points", "TEPlugName=OGR;FileName=" + gpsPointsFile, ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
            gpsTrailFeatureLayer.getDataSourceInfo().getAttributes().setImportAll(true);
            ISGWorld.getInstance().getProjectTree().SetVisibility(gpsTrailFeatureLayer.getID(), false);
            try {
                gpsTrailFeatureLayer.Load();
            } catch (Exception e) {

            }
            gpsTrailFeatureLayerSaveQueued = false;
        }

        IFeatureGroup featureGroup = ((TEIUnknownHandle) gpsTrailFeatureLayer.getFeatureGroups().get_Item(0)).CastTo(IFeatureGroup.class);
        IPoint geom = ISGWorld.getInstance().getCreator().getGeometryCreator().CreatePointGeometry(new double[]{location.getLongitude(), location.getLatitude(), 0});
        featureGroup.CreateFeature(geom, String.format("%d", location.getTime()));

        // save gps points layer once per 30 seconds
        if (gpsTrailFeatureLayerSaveQueued == false) {
            gpsTrailFeatureLayerSaveQueued = true;
            // there is no looper on render thread, so use main thread looper and come back to render thread
            // ugly :(
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    UI.runOnRenderThread(new Runnable() {
                        @Override
                        public void run() {
                            if (gpsTrailFeatureLayerSaveQueued)
                                gpsTrailFeatureLayer.Save();
                            gpsTrailFeatureLayerSaveQueued = false;
                        }
                    });
                }
            }, 30 * 1000);
        }
    }

    private void settingChanged(Intent intent) {
        int key = intent.getIntExtra(SettingsTool.SETTING_NAME, 0);
        if (key == R.string.key_my_position) {
            showUserPosition = intent.getBooleanExtra(SettingsTool.SETTING_VALUE, showUserPosition);
            updateTrackerState();
        }
        if (key == R.string.key_gps_trail) {
            showGpsTrail = intent.getBooleanExtra(SettingsTool.SETTING_VALUE, showGpsTrail);
            updateTrackerState();
        }
    }

    private IPosition location2position(Location location) {
        return ISGWorld.getInstance().getCreator().CreatePosition(location.getLongitude(), location.getLatitude(), location.getAltitude(), AltitudeTypeCode.ATC_ON_TERRAIN, location.getBearing(), 75, 0, 10000);
    }


}
