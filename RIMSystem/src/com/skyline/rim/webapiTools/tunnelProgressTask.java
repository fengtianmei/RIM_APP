package com.skyline.rim.webapiTools;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.util.Hashtable;
import java.util.List;

import static com.skyline.rim.globaldata.ConstData.HHANDLER_SGJD_ATTRS;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_SGJD_PRO;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_SYS_FINISHED;
import static com.skyline.rim.globaldata.ConstData.QUERY_OBJ_ATTRS;
import static com.skyline.rim.globaldata.ConstData.QUERY_OBJ_PRO;
import static com.skyline.rim.globaldata.ConstData.REQUEST_POST;
import static com.skyline.rim.globaldata.UserInfo.mProjectID;

/**
 * Created by mft on 2017/10/31.
 */

public class tunnelProgressTask extends objProgressTask {
    /***
     * 服务器进度信息获取类
     * @param type 进度信息的类型1=目标的进度属性信息；3=目标的请求描述信息
     * @param layercount 图层数量
     * @param featureAttributes 目标的属性信息，当type为3的时候，可以设置为空参数
     * @param name 目标名
     * @param dtime 日期
     */
    public tunnelProgressTask(int type, int layercount, List<List> featureAttributes, String name, String dtime) {
        super(type, layercount, featureAttributes, name, dtime);
    }

    /***
     * 获取对象的进度信息
     * @param handlerProgress 接收消息的句柄
     */
    public void getObjProgressInfo(Handler handlerProgress) {
        this.handlerProgress = handlerProgress;
        String urlString = "http://192.168.4.4:83/api/rim/Progress/SgTunnelPlan";
        //构建查询条件
        JSONObject obj = new JSONObject();
        //构建webapi请求对象,获取对象的属性信息
        if (type == QUERY_OBJ_ATTRS) {//构建属性信息，供查询使用
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                for (int i = 0; i < featureAttributes.size(); i++) {
                    stringBuilder.append("[");
                    for (int j = 0; j < featureAttributes.get(i).size(); j++) {
                        Hashtable<String, String> hObj = (Hashtable<String, String>) featureAttributes.get(i).get(j);
                        stringBuilder.append("{\"lc\":\"" + hObj.get("lc") + "\",\"mtype\":\"" + hObj.get("mtype") + "\",\"mlen\":\"" + hObj.get("mlen") + "\"},");
                    }
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    stringBuilder.append("],");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                stringBuilder.append("]");
                obj.put("prjID",mProjectID);
                obj.put("type", this.type);
                obj.put("layercount", this.layercount);
                obj.put("featureAttributes", stringBuilder.toString());
                obj.put("gdname", this.name);
                obj.put("dtime", this.dtime);
            } catch (Exception e) {
                Log.e("tsdi", e.getMessage());
                //webapi信息请求失败，直接退出
                Message message = new Message();
                message.what = HHANDLER_SYS_FINISHED;//事件结束标志  
                handlerProgress.sendMessage(message);//发送message信息
            }
            threadTask = new webapiTask(urlString, handlerProgress, HHANDLER_SGJD_ATTRS, REQUEST_POST, obj);
        } else if (type == QUERY_OBJ_PRO) {//查询描述信息
            //查询描述信息
            try {
                obj.put("prjID",mProjectID);
                obj.put("type", this.type);
                obj.put("layercount", 1);
                obj.put("featureAttributes", "qq");
                obj.put("gdname", this.name);
                obj.put("dtime", this.dtime);
            } catch (Exception e) {
                Log.e("tsdi", e.getMessage());
                //webapi信息请求失败，直接退出
                Message message = new Message();
                message.what = HHANDLER_SYS_FINISHED;//事件结束标志  
                handlerProgress.sendMessage(message);//发送message信息
            }
            threadTask = new webapiTask(urlString, handlerProgress, HHANDLER_SGJD_PRO, REQUEST_POST, obj);
        }
        //开始执行网络请求
        threadTask.execute((Void) null);
    }
}
