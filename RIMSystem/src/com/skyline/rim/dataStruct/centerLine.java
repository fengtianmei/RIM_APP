package com.skyline.rim.dataStruct;

/**
 * Created by mft on 2017/10/19.
 * 铁路设计中线对象
 */

public class centerLine {
    public int getLineID() {
        return lineID;
    }

    public void setLineID(int lineID) {
        this.lineID = lineID;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    private int lineID;
    private String lineName;
}
