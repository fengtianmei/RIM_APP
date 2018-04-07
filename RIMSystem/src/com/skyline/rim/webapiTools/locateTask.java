package com.skyline.rim.webapiTools;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.skyline.teapi.AltitudeTypeCode;
import com.skyline.teapi.ISGWorld;
import com.skyline.terraexplorer.models.UI;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.skyline.rim.globaldata.ConstData.HHANDLER_CENTER_LOCATE;
import static com.skyline.rim.globaldata.UserInfo.mProjectID;
import static com.skyline.rim.globaldata.systemInfo.teActivity;

/**
 * Created by mft on 2017/10/19.
 * 根据里程定位地图
 */
public class locateTask {
    private webapiTask threadTask = null;
    private JSONObject jsonObject = null;
    //定义Handler对象接收子线程消息
    private Handler handler = new Handler() {
        @Override
        //当有消息发送出来的时候就执行Handler的这个方法
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0://请求失败
                    break;
                case HHANDLER_CENTER_LOCATE://请求成功
                    //根据服务器端返回的jsonObject给centerLines赋值
                    try {
                        jsonObject = new JSONObject(msg.getData().getString("jsonObj"));
                        String code = jsonObject.getString("code");
                        //根据code判断是否登陆成功
                        if (Integer.parseInt(code) != 1) {

                        } else {
                            //从返回的json数据里，可以用getJSONArray获取相关参数，并记录下列信息到缓存
                            JSONArray jsonArray = new JSONArray(jsonObject.getString("data"));
                            if (jsonArray.length() > 0) {
                                JSONObject item = jsonArray.getJSONObject(0); // 得到每个对象
                                final double x = item.getDouble("X");
                                final double y = item.getDouble("Y");
                                final double h = item.getDouble("H");
                                if (teActivity != null && ISGWorld.getInstance() != null) {
                                    UI.runOnRenderThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ISGWorld.getInstance().getNavigate().FlyTo(ISGWorld.getInstance().getCreator().CreatePosition(x, y, h + 10, AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE, 0, -50, 0, 150));
                                        }
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("tsdi", e.getMessage());
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public void locateTo(float location, int lineID) {
        jsonObject = new JSONObject();
        String requestString = "http://192.168.4.4:83/api/rim/Mileage/getCenterLineLocation?prjID=" + mProjectID + "&lc=" + location + "&lineid=" + lineID;
        //构建请求字符串
        threadTask = new webapiTask(requestString, handler, HHANDLER_CENTER_LOCATE);
        threadTask.execute((Void) null);
    }
}
