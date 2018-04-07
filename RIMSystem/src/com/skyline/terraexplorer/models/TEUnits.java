package com.skyline.terraexplorer.models;

import com.skyline.teapi.ISGWorld;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;

import java.util.concurrent.Callable;

public class TEUnits {
    public static final int UNITS_METER = 0;
    public static final int UNITS_FEET = 1;

    public static final TEUnits instance = new TEUnits();

    private TEUnits() {
    }

    public String formatDistance(double distance) {
        return formatDistance(distance, true);
    }

    public String formatDistance(double distance, boolean convertToDisplayUnits) {
        if (getUnitsType() == UNITS_METER) {
            if (Math.abs(distance) < 10000)
                return format(distance, TEApp.getAppContext().getString(R.string.meters));
            else
                return format(distance / 1000, TEApp.getAppContext().getString(R.string.km));
        } else {
            if (convertToDisplayUnits)
                distance = meter2feet(distance);

            if (Math.abs(distance) < 5280)
                return format(distance, TEApp.getAppContext().getString(R.string.feet));
            else
                return format(distance / 5280, TEApp.getAppContext().getString(R.string.mile));
        }
    }

    private double meter2feet(double meter) {
        return meter * 3.28084;
    }

    public String formatDistanceAbbr(double distance) {
        return formatDistanceAbbr(distance, true);
    }

    public String formatDistanceAbbr(double distance, boolean convertToDisplayUnits) {
        if (getUnitsType() == UNITS_METER) {
            if (Math.abs(distance) < 10000)
                return format(distance, TEApp.getAppContext().getString(R.string.meters_abbr));
            else
                return format(distance / 1000, TEApp.getAppContext().getString(R.string.km_abbr));
        } else {
            if (convertToDisplayUnits)
                distance = meter2feet(distance);

            if (Math.abs(distance) < 5280)
                return format(distance, TEApp.getAppContext().getString(R.string.feet_abbr));
            else
                return format(distance / 5280, TEApp.getAppContext().getString(R.string.mile_abbr));
        }
    }

    public String formatArea(double area) {
        return formatArea(area, true);
    }

    public String formatArea(double area, boolean convertToDisplayUnits) {
        if (getUnitsType() == UNITS_METER) {
            if (Math.abs(area) < 1000 * 1000)
                return format(area, TEApp.getAppContext().getString(R.string.square_meter));
            else
                return format(area / (1000 * 1000), TEApp.getAppContext().getString(R.string.square_km));
        } else {
            if (convertToDisplayUnits)
                area = area * 10.7639f;

            if (Math.abs(area) < 5280 * 5280)
                return format(area, TEApp.getAppContext().getString(R.string.square_feet));
            else
                return format(area / (5280 * 5280), TEApp.getAppContext().getString(R.string.square_mile));
        }
    }

    public void setUnitsType(final int unitType) {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().SetOptionParam("Speed", unitType);
                ISGWorld.getInstance().SetOptionParam("AltitudeAndDistance", unitType);
            }
        });
    }

    public int getUnitsType() {
        return UI.runOnRenderThread(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return (Integer) ISGWorld.getInstance().GetOptionParam("AltitudeAndDistance");
            }
        });
    }

    private String format(double number, String unitName) {
        return String.format("%.2f %s", number, unitName);
    }
}
