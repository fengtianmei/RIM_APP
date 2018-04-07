package com.skyline.terraexplorer.models;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialogDelegateBase;

import java.util.List;

public class LocationTracker {

    public interface LocationTrackerDelegate {
        public void locationTrackerStateChanged(boolean connected);

        public void locationUpdate(Location location);
    }

    private static String NO_GPS_TAG = "NO_GPS";
    private LocationManager locationManager;
    private boolean connected = false;
    private boolean simulate = false;
    private boolean askedToEnableGps = false;
    private LocationTrackerDelegate delegate = new LocationTrackerDelegate() {

        @Override
        public void locationTrackerStateChanged(boolean connected) {
        }

        @Override
        public void locationUpdate(Location location) {
        }
    };

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
            resume();
        }

        @Override
        public void onProviderDisabled(String s) {
        }

        @Override
        public void onLocationChanged(Location location) {
            delegate.locationUpdate(location);
        }
    };

    private ModalDialogDelegateBase modalDelegate = new ModalDialogDelegateBase() {
        public void modalDialogDidDismissWithOk(ModalDialog dlg) {
            LocationTracker.this.modalDialogDidDismissWithOk(dlg);
        }

        ;
    };

    public LocationTracker() {
        locationManager = (LocationManager) TEApp.getAppContext()
                .getSystemService(Context.LOCATION_SERVICE);
    }

    public void toggleConnectionState() {
        if (connected)
            disconnect();
        else
            connect();
    }

    public void disconnect() {
        pause();
        connected = false;
        askedToEnableGps = false;
        delegate.locationTrackerStateChanged(connected);
    }

    public boolean isGpsAvaliable(boolean showError) {
        boolean gpsAvaliable = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER);
        if (gpsAvaliable == false && showError) {
            showGPSUnavalibaleAlert();
        }
        return gpsAvaliable;
    }

    public void connect() {

        connected = true;
        resume();
        delegate.locationTrackerStateChanged(connected);
    }

    public void pause() {
        if (connected)
            locationManager.removeUpdates(locationListener);
    }

    public void resume() {
        if (connected) {
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isNetworkEnabled == false && isGpsEnabled == false) {
                showLocationServicesDisabledAlert(false);
                disconnect();
                return;
            }
            if (isGpsEnabled == false) {
                showLocationServicesDisabledAlert(true);
            }
            if (simulate == false) {
                if (isNetworkEnabled)
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 400, 1, locationListener);
                if (isGpsEnabled)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, locationListener);
            } else {
                simulateLocation();
            }
        }
    }

    private void showGPSUnavalibaleAlert() {
        ModalDialog dlg = new ModalDialog(R.string.locationTracker_alert_nogps_title, modalDelegate);
        dlg.setTag(LocationTracker.NO_GPS_TAG);
        dlg.setContentMessage(R.string.locationTracker_alert_nogps_message);
        dlg.setOneButtonMode();
        dlg.show();
    }

    ;

    private void showLocationServicesDisabledAlert(boolean gpsOnly) {
        if (gpsOnly && askedToEnableGps)
            return;
        askedToEnableGps = true;
        ModalDialog dlg = new ModalDialog(R.string.locationTracker_alert_title, modalDelegate);
        dlg.setContentMessage(gpsOnly ? R.string.locationTracker_alert_message_gps_only : R.string.locationTracker_alert_message);
        dlg.setCancelButtonTitle(R.string.cancel);
        dlg.setOkButtonTitle(R.string.locationTracker_enable);
        dlg.show();
    }

    ;

    private void modalDialogDidDismissWithOk(ModalDialog dlg) {
        if (dlg.getTag() == NO_GPS_TAG)
            return;
        Intent intent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        TEApp.getCurrentActivityContext().startActivity(intent);
    }

    public boolean isConnected() {
        return connected;
    }

    public Location getLastKnownLocation() {
        Location bestResult = null;
        float bestAccuracy = Float.MAX_VALUE;
        long bestTime = Long.MIN_VALUE;

        List<String> matchingProviders = locationManager.getAllProviders();
        for (String provider : matchingProviders) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if (accuracy < bestAccuracy) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                } else if (bestAccuracy == Float.MAX_VALUE && time > bestTime) {
                    bestResult = location;
                    bestTime = time;
                }
            }
        }
        if (bestResult != null)
            return new Location(bestResult);
        return null;
    }

    private static int TEALocationTracker_simulateIndex = 0;
    private static double TEALocationTracker_prevX = 34.783636;
    private static double TEALocationTracker_prevY = 32.065983;
    private static Handler handler = new Handler(Looper.getMainLooper());

    private void simulateLocation() {
        if (connected) {
            double distance = 0.001 * TEALocationTracker_simulateIndex;
            switch (TEALocationTracker_simulateIndex % 4) {
                case 0:
                    TEALocationTracker_prevX = TEALocationTracker_prevX + distance;
                    break;
                case 1:
                    TEALocationTracker_prevY = TEALocationTracker_prevY + distance;
                    break;
                case 2:
                    TEALocationTracker_prevX = TEALocationTracker_prevX - distance;
                    break;
                case 3:
                    TEALocationTracker_prevY = TEALocationTracker_prevY - distance;
                    break;

                default:
                    break;
            }
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(TEALocationTracker_prevY);
            location.setLongitude(TEALocationTracker_prevX);
            delegate.locationUpdate(location);
            TEALocationTracker_simulateIndex++;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    simulateLocation();
                }
            }, 1000);
        }
    }

    public void setDelegate(LocationTrackerDelegate delegate) {
        this.delegate = delegate;
    }
}
