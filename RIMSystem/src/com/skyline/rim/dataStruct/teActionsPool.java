package com.skyline.rim.dataStruct;

import com.skyline.teapi.AltitudeTypeCode;
import com.skyline.teapi.IPosition;
import com.skyline.teapi.ISGWorld;
/**
 * JS调用Skyline的动作缓冲池，记录将要执行的动作
 * Created by mft on 2017/10/31.
 */

public class teActionsPool {
    public String getFlyToBridge() {
        return flyToBridge;
    }

    public void setFlyToBridge(String flyToBridge) {
        this.flyToBridge = flyToBridge;
    }

    //将要飞到的桥的名字
    private String flyToBridge = "";

    public String getFlyToTunnel() {
        return flyToTunnel;
    }

    public void setFlyToTunnel(String flyToTunnel) {
        this.flyToTunnel = flyToTunnel;
    }

    //将要飞到的桥的名字
    private String flyToTunnel = "";

    public String getProgressBridge() {
        return progressBridge;
    }

    public void setProgressBridge(String progressBridge) {
        this.progressBridge = progressBridge;
    }

    //将要打开进度信息的桥的名字
    private String progressBridge = "";

    public String getProgressTunnel() {
        return progressTunnel;
    }

    public void setProgressTunnel(String progressTunnel) {
        this.progressTunnel = progressTunnel;
    }

    //将要打开进度信息的隧道的名字
    private String progressTunnel = "";

    public String getFlyFileUrl() {
        return flyFileUrl;
    }

    public void setFlyFileUrl(String flyFileUrl) {
        this.flyFileUrl = flyFileUrl;
    }

    //将要打开的fly文件地址,在OnCreate和onReStart消息里将被调用（主要用来相应JS接口的请求）
    private String flyFileUrl = "";

    public IPosition getFlyToPos() {
        return flyToPos;
    }

    public void setFlyToPos(final double x, final double y) {
        if (ISGWorld.getInstance() != null)
            flyToPos = ISGWorld.getInstance().getCreator().CreatePosition(x, y, 30, AltitudeTypeCode.ATC_ON_TERRAIN, 0, -50, 0, 150);
    }

    public void setFlyToPos(IPosition pos) {
        flyToPos = pos;
    }

    //将要飞往的地点,在onReStart消息里将被调用（主要用来相应JS接口的请求）
    private IPosition flyToPos = null;

    public int getTe_new_function_type() {
        return te_new_function_type;
    }

    public void setTe_new_function_type(int te_new_function_type) {
        this.te_new_function_type = te_new_function_type;
    }

    //将要打开的skyline界面的功能类型,在onReStart消息里将被调用（主要用来相应JS接口的请求）
    private int te_new_function_type = -1;
}
