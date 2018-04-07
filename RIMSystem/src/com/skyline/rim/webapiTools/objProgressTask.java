package com.skyline.rim.webapiTools;

import android.os.Handler;
import android.os.Message;

import org.json.JSONObject;

import java.util.List;

/**
 * 进度信息请求的基类。不要直接调用此类，请调用派生类实现具体的进度信息获取
 * Created by mft on 2017/10/25.
 */
public class objProgressTask {
    //webapi请求对象
    public webapiTask threadTask = null;
    //存储webapi查询的结果
    public JSONObject jsonObject = null;
    //查询条件
    public List<List> featureAttributes;
    public String name;
    public int type;
    public String dtime;
    public int layercount;
    //上级对象接收请求结果的句柄
    public Handler handlerProgress = null;

    /***
     * 服务器进度信息获取类
     * @param type 进度信息的类型1=目标的进度属性信息；3=目标的请求描述信息
     * @param layercount 图层数量
     * @param featureAttributes 目标的属性信息，当type为3的时候，可以设置为空参数
     * @param name 目标名
     * @param dtime 日期
     */
    public objProgressTask(int type, int layercount, List<List> featureAttributes, String name, String dtime) {
        this.featureAttributes = featureAttributes;
        this.name = name;
        this.dtime = dtime;
        this.type = type;
        this.layercount = layercount;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    //定义Handler对象接收子线程消息
    public Handler handler = new Handler() {
        @Override
        //当有消息发送出来的时候就执行Handler的这个方法
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            if (parentHhandler == null)
//                return;
//            switch (msg.what) {
//                case 0://请求失败
//                    parentHhandler.sendEmptyMessage(0);
//                    break;
//                case HHANDLER_SGJD_ATTRS://请求成功
//                    //根据服务器端返回的jsonObject给获取对象的进度信息
//                    try {
//                        Message message = new Message();
//                        Bundle bundle = new Bundle();
//                        bundle.putString("jsonObj", msg.getData().getString("jsonObj"));
//                        message.setData(bundle);//bundle传值，耗时，效率低
//                        message.what = HHANDLER_SGJD_ATTRS;//标志是哪个线程传数据  
//                        parentHhandler.sendMessage(message);//发送message信息 
//                    } catch (Exception e) {
//                        Log.e("tsdi", e.getMessage());
//                    }
//                    break;
//                case HHANDLER_SGJD_PRO://请求成功
//                    //根据服务器端返回的jsonObject给获取对象的进度信息
//                    try {
//                        Message message = new Message();
//                        Bundle bundle = new Bundle();
//                        bundle.putString("jsonObj", msg.getData().getString("jsonObj"));
//                        message.setData(bundle);//bundle传值，耗时，效率低
//                        message.what = HHANDLER_SGJD_PRO;//标志是哪个线程传数据  
//                        parentHhandler.sendMessage(message);//发送message信息 
//                    } catch (Exception e) {
//                        Log.e("tsdi", e.getMessage());
//                    }
//                    break;
//                default:
//                    break;
//            }
        }
    };
    /***
     * 获取对象的进度信息
     * @param handlerProgress 接收消息的句柄
     */
    public void getObjProgressInfo(Handler handlerProgress) {
    }
}
