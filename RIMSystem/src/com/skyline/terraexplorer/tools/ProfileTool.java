package com.skyline.terraexplorer.tools;

import android.content.Intent;

import com.skyline.teapi.AltitudeTypeCode;
import com.skyline.teapi.ApiException;
import com.skyline.teapi.IGeometry;
import com.skyline.teapi.ILineString;
import com.skyline.teapi.IPoint;
import com.skyline.teapi.IPosition;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.TerrainProfileActivity;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

import java.io.Serializable;

public class ProfileTool extends DistanceTool {

    public static class TerrainProfileData implements Serializable {
        private static final long serialVersionUID = 1L;
        public float aerialDistance;
        public float horizontalDistance;
        public float groundDistance;
        public float[] samplePoints;
        public int[] wayPoints;
        public int maxAltitudeIndex;
        public int minAltitudeIndex;
        public int maxSlopeIndex;
        public int minSlopeIndex;
    }

    public static final String PROFILE_DATA = "com.skyline.terraexplorer.ProfileTool.PROFILE_DATA";

    private TerrainProfileData calculatedProfile;
    private double aerialDistance;
    private double horizontalDistance;

    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.mm_analyze_profile, R.drawable.profile, MenuEntry.MenuEntryAnalyze(), 35);
    }

    public ProfileTool() {
        commandId = 1043;
    }

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        calculatedProfile = null;
        return super.onBeforeCloseToolContainer(closeReason);
    }

    @Override
    protected void showNormalButtons() {
        toolContainer.removeButtons();
        toolContainer.addButton(1, R.drawable.delete);
        toolContainer.addButton(2, R.drawable.delete_last_point);
        toolContainer.addButton(3, R.drawable.profile);
    }

    @Override
    protected void doWork() {
        if (calculatedProfile == null) {
            ISGWorld.getInstance().addOnAnalysisProgressListener(this);
            // currentGeometry may be point
            if (currentGeometry.CastTo(ILineString.class) != null) {
                ILineString profileGeometry = null;
                // ignore api exception that is thrown on cancel
                try {
                    profileGeometry = ISGWorld.getInstance().getAnalysis().MeasureTerrainProfile(currentGeometry, 0, true).CastTo(ILineString.class);
                } catch (ApiException e) {
                    if (this.workCanceled == false)
                        throw e;
                }
                calculatedProfile = calculateTerrainProfile(profileGeometry);
            }
            ISGWorld.getInstance().removeOnAnalysisProgressListener(this);
        }
    }

    @Override
    protected void workCompleted() {
        if (workCanceled == false && calculatedProfile != null) {
            Intent intent = new Intent(TEApp.getCurrentActivityContext(), TerrainProfileActivity.class);
            intent.putExtra(PROFILE_DATA, calculatedProfile);
            TEApp.getCurrentActivityContext().startActivity(intent);
        }
    }

    private TerrainProfileData calculateTerrainProfile(ILineString profileGeometry) {
        if (profileGeometry == null || currentGeometry == null)
            return null;

        ILineString wayPointsGeometry = currentGeometry.CastTo(ILineString.class);

        TerrainProfileData profile = new TerrainProfileData();
        profile.aerialDistance = (float) aerialDistance;
        profile.horizontalDistance = (float) horizontalDistance;
        profile.wayPoints = new int[wayPointsGeometry.getPoints().getCount()];
        profile.samplePoints = new float[profileGeometry.getPoints().getCount() * 4];
        profile.minSlopeIndex = -1;
        profile.maxSlopeIndex = -1;
        profile.minAltitudeIndex = -1;
        profile.maxAltitudeIndex = -1;

        // profileGeometry contains both samplepoints and waypoints
        // convert sample points to array of z values
        // mark index of each way point
        int nextWayPointIndex = 0;
        IPoint nextWayPoint = ((TEIUnknownHandle) wayPointsGeometry.getPoints().get_Item(nextWayPointIndex)).CastTo(IPoint.class);
        for (int i = 0; i < profileGeometry.getPoints().getCount(); i++) {
            IPoint samplePoint = ((TEIUnknownHandle) profileGeometry.getPoints().get_Item(i)).CastTo(IPoint.class);
            if (nextWayPoint != null && samplePoint.getX() == nextWayPoint.getX() && samplePoint.getY() == nextWayPoint.getY()) {
                profile.wayPoints[nextWayPointIndex] = i;
                nextWayPointIndex++;
                if (nextWayPointIndex < wayPointsGeometry.getPoints().getCount())
                    nextWayPoint = ((TEIUnknownHandle) wayPointsGeometry.getPoints().get_Item(nextWayPointIndex)).CastTo(IPoint.class);
                else
                    nextWayPoint = null;
            }

            // check if we have min/max altitude
            if (profile.maxAltitudeIndex == -1 || samplePoint.getZ() > profile.samplePoints[4 * profile.maxAltitudeIndex + 2]) {
                profile.maxAltitudeIndex = i;
            }
            if (profile.minAltitudeIndex == -1 || samplePoint.getZ() < profile.samplePoints[4 * profile.minAltitudeIndex + 2]) {
                profile.minAltitudeIndex = i;
            }

            // calculate min/max slope
            float slope = 0;
            if (i > 0) {
                slope = (float) (180 / Math.PI * (Math.atan2(samplePoint.getZ() - profile.samplePoints[(i - 1) * 4 + 2], 1.0)));
                if (profile.maxSlopeIndex == -1 || slope > profile.samplePoints[4 * profile.maxSlopeIndex + 3]) {
                    profile.maxSlopeIndex = i - 1;
                }
                if (profile.minSlopeIndex == -1 || slope < profile.samplePoints[4 * profile.minSlopeIndex + 3]) {
                    profile.minSlopeIndex = i - 1;
                }
            }

            // calculate ground distance
            if (i > 0) {
                double x = profile.samplePoints[(i - 1) * 4 + 0];
                double y = profile.samplePoints[(i - 1) * 4 + 1];
                double z = profile.samplePoints[(i - 1) * 4 + 2];
                IPosition pos1 = ISGWorld.getInstance().getCreator().CreatePosition(x, y, z, AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE);
                IPosition pos2 = ISGWorld.getInstance().getCreator().CreatePosition(samplePoint.getX(), samplePoint.getY(), samplePoint.getZ(), AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE);
                double groundDistance = pos1.DistanceTo(pos2);
                profile.groundDistance += groundDistance;
            }
            profile.samplePoints[(i) * 4 + 0] = (float) samplePoint.getX();
            profile.samplePoints[(i) * 4 + 1] = (float) samplePoint.getY();
            profile.samplePoints[(i) * 4 + 2] = (float) samplePoint.getZ();
            profile.samplePoints[(i) * 4 + 3] = slope;

            if (workCanceled)
                return null;
        }
        return profile;
    }

    @SuppressWarnings("unused")
    private TerrainProfileData calculateTerrainProfileTest() {
        int numberOfPoints = 1000;
        TerrainProfileData profile = new TerrainProfileData();
        profile.aerialDistance = (float) aerialDistance;
        profile.horizontalDistance = (float) horizontalDistance;
        profile.wayPoints = new int[10];
        profile.samplePoints = new float[numberOfPoints * 4];
        profile.minSlopeIndex = -1;
        profile.maxSlopeIndex = -1;
        profile.minAltitudeIndex = -1;
        profile.maxAltitudeIndex = -1;

        for (int i = 0; i < numberOfPoints; i++) {
            if (i % (numberOfPoints / 10) == 0) {
                profile.wayPoints[i / (numberOfPoints / 10)] = i;
            }
            float X = i;
            float Y = i;
            float Z = (float) Math.sin(X * Math.PI / 180);
            //float Z = (float)Math.sin(X) + X*X/100 + 10;
            // check if we have min/max altitude
            if (profile.maxAltitudeIndex == -1 || Z > profile.samplePoints[4 * profile.maxAltitudeIndex + 2]) {
                profile.maxAltitudeIndex = i;
            }
            if (profile.minAltitudeIndex == -1 || Z < profile.samplePoints[4 * profile.minAltitudeIndex + 2]) {
                profile.minAltitudeIndex = i;
            }

            // calculate min/max slope
            float slope = 0;
            if (i > 0) {
                slope = (float) (180 / Math.PI * (Math.atan2(Z - profile.samplePoints[(i - 1) * 4 + 2], 1.0)));
                if (profile.maxSlopeIndex == -1 || slope > profile.samplePoints[4 * profile.maxSlopeIndex + 3]) {
                    profile.maxSlopeIndex = i - 1;
                }
                if (profile.minSlopeIndex == -1 || slope < profile.samplePoints[4 * profile.minSlopeIndex + 3]) {
                    profile.minSlopeIndex = i - 1;
                }
            }
            profile.samplePoints[(i) * 4 + 0] = X;
            profile.samplePoints[(i) * 4 + 1] = Y;
            profile.samplePoints[(i) * 4 + 2] = Z;
            profile.samplePoints[(i) * 4 + 3] = slope;

            if (workCanceled)
                return null;
        }
        return profile;
    }

    @Override
    public void OnAnalysisDistancePointAdded(IGeometry geom, double aerial,
                                             double horizontal, double slope, double elevDiff) {
        super.OnAnalysisDistancePointAdded(geom, aerial, horizontal, slope, elevDiff);
        calculatedProfile = null;
        aerialDistance = aerial;
        horizontalDistance = horizontal;
    }
}
