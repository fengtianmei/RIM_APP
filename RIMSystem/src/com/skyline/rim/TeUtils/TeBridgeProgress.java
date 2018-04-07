package com.skyline.rim.TeUtils;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.skyline.rim.dataStruct.bridgeAttr;
import com.skyline.rim.sqlite.rimDB;
import com.skyline.rim.webapiTools.BridgeProgressTask;
import com.skyline.rim.webapiTools.objProgressTask;
import com.skyline.teapi.I3DMLFeatureLayer;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.IFeatures;
import com.skyline.teapi.IGeometry;
import com.skyline.teapi.ILinearRing;
import com.skyline.teapi.IMeshLayer;
import com.skyline.teapi.IPolygon;
import com.skyline.teapi.IProjectTree;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.UI;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import static com.skyline.rim.TeUtils.TeBase.flyToObjectByName;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_FILLPROGRESS_OVER;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_GET_MESHLAYER;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_PROGRESS_RENDING;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_PROGRESS_START;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_SAVEPROGRESS_LOCAL;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_SGJD_ATTRS;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_SGJD_PRO;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_SYS_FINISHED;
import static com.skyline.rim.globaldata.ConstData.OBJ_3D_BRIDGE;
import static com.skyline.rim.globaldata.ConstData.OPENDB_WRITEABLE;
import static com.skyline.rim.globaldata.ConstData.QUERY_OBJ_ATTRS;
import static com.skyline.rim.globaldata.ConstData.QUERY_OBJ_PRO;
import static com.skyline.rim.globaldata.UserInfo.mProjectID;
import static com.skyline.rim.globaldata.systemInfo.teActivity;
import static com.skyline.terraexplorer.TEApp.getCurrentActivityContext;

/**
 * 桥进度展示类
 * Created by mft on 2017/10/30.
 */

public class TeBridgeProgress extends TeProgress {

    /***
     * 获取目标桥的进度信息并显示。步骤：1.获取空间信息Get3DMLLayer-->2.请求服务器端信息bridgeProgressTask--->3.把服务器端的信息叠加到skyling对象，并进行显示
     * @param objName 对象名字
     * @param showType 进度类型（实际、计划）PROGRESS_SHOW_ACTUAL/PROGRESS_SHOW_PLAN
     * @param strDate 统计时间
     */
    public void show(String objName, int showType, String strDate) {
        this.setDownLoadOnly(false);
        gdName = objName;
        this.showType = showType;
        this.strDate = strDate;
        objType = OBJ_3D_BRIDGE;
        if (teActivity != null)
            pd = ProgressDialog.show(teActivity, teActivity.getString(R.string.info_app_progress_title), teActivity.getString(R.string.info_app_progress_ing));
        Message message = new Message();
        message.what = HHANDLER_PROGRESS_START;//标志是哪个线程传数据  
        handlerProgress.sendMessage(message);//发送message信息 
    }

    /***
     * 下载进度数据并存储到本地数据库。暂时不用这个函数
     * @param objName
     * @param showType
     * @param strDate
     */
    public void downLoadProgress(String objName, int showType, String strDate) {
        this.setDownLoadOnly(true);
        gdName = objName;
        this.showType = showType;
        this.strDate = strDate;
        objType = OBJ_3D_BRIDGE;
        Message message = new Message();
        message.what = HHANDLER_PROGRESS_START;//标志是哪个线程传数据  
        handlerProgress.sendMessage(message);//发送message信息 
    }

    /***
     * 根据进度信息填充skyling进度数据
     * @param jsonAttrs 目标的属性数据
     * @param jsonPro 目标的描述数据
     */
    public void FillRealProgressDatetable(final JSONObject jsonAttrs, final JSONObject jsonPro) {
        //计划进度赋值
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                try {

                    JsonToAttrs(jsonAttrs, dateList);
                    JSONObject bridgepro = new JSONObject(jsonPro.getString("data"));
                    for (int i = 0; i < dateList.size(); i++) {
                        //逐行获取属性值
                        for (int j = 0; j < dateList.get(i).size(); j++) {
                            ((bridgeAttr) dateList.get(i).get(j)).EndDate = ((bridgeAttr) dateList.get(i).get(j)).StartDate = "21000101";
                            int type = Integer.parseInt(((Hashtable<String, String>) featureAttributes.get(i).get(j)).get("qType")); //// Convert.ToDouble(atibute.GetFeatureAttribute(kmcolname).Value);
                            int NO = Integer.parseInt(((Hashtable<String, String>) featureAttributes.get(i).get(j)).get("index1"));
                            int index2 = Integer.parseInt(((Hashtable<String, String>) featureAttributes.get(i).get(j)).get("index2"));//实际进度中，设置0；
                            int index3 = Integer.parseInt(((Hashtable<String, String>) featureAttributes.get(i).get(j)).get("index3"));

                            if (type == 101)//桩
                            {
                                //单个桩的工期
                                int pegfinum = (int) ((JSONObject) ((JSONArray) bridgepro.get("m_pier")).get(NO - 1)).get("m_pegfinnum");
                                if (index2 <= pegfinum)
                                    ((bridgeAttr) dateList.get(i).get(j)).EndDate = ((bridgeAttr) dateList.get(i).get(j)).StartDate =
                                            ((String) ((JSONObject) ((JSONArray) bridgepro.get("m_pier")).get(NO - 1)).get("m_inputdate")).replace("-", "");
                            } else if (type == 102) //承台
                            {

                                //单个承台的工期
                                int capfinum = (int) ((JSONObject) ((JSONArray) bridgepro.get("m_pier")).get(NO - 1)).get("m_basefinnum");
                                if (index2 <= capfinum)
                                    ((bridgeAttr) dateList.get(i).get(j)).EndDate = ((bridgeAttr) dateList.get(i).get(j)).StartDate =
                                            ((String) ((JSONObject) ((JSONArray) bridgepro.get("m_pier")).get(NO - 1)).get("m_inputdate")).replace("-", "");
                            } else if (type == 103) //墩身
                            {

                                //单个墩身块的工期
                                float bottomh = Float.parseFloat(((Hashtable<String, String>) featureAttributes.get(i).get(j)).get("pos"));
                                float length = Float.parseFloat(((Hashtable<String, String>) featureAttributes.get(i).get(j)).get("qLength"));
                                if ((bottomh + length) <= Float.parseFloat(((JSONObject) ((JSONArray) bridgepro.get("m_pier")).get(NO - 1)).get("m_bodyfinh").toString()) + 0.1) {
                                    ((bridgeAttr) dateList.get(i).get(j)).EndDate = ((bridgeAttr) dateList.get(i).get(j)).StartDate =
                                            ((String) ((JSONObject) ((JSONArray) bridgepro.get("m_pier")).get(NO - 1)).get("m_inputdate")).replace("-", "");
                                }
                            } else if (type == 104) //垫石
                            {
                                if ((double) ((JSONObject) ((JSONArray) bridgepro.get("m_pier")).get(NO - 1)).get("m_bodyfinh") >= (double) ((JSONObject) (((JSONObject) ((JSONArray) bridgepro.get("m_pier")).get(NO - 1)).get("m_data"))).get("m_fbodyh"))
                                    ((bridgeAttr) dateList.get(i).get(j)).EndDate = ((bridgeAttr) dateList.get(i).get(j)).StartDate = ((String) ((JSONObject) ((JSONArray) bridgepro.get("m_pier")).get(NO - 1)).get("m_inputdate")).replace("-", "");
                            } else if (type == 110 || type == 111 || type == 112 || type == 114) //梁
                            {

                                float lich = ((bridgeAttr) dateList.get(i).get(j)).Pos;
                                if ((Float.parseFloat(((JSONObject) ((JSONArray) bridgepro.get("m_girder")).get(NO)).get("m_finekm").toString()) - lich) * (Float.parseFloat((((JSONObject) ((JSONArray) bridgepro.get("m_girder")).get(NO)).get("m_finskm")).toString()) - lich) <= 0) {
                                    ((bridgeAttr) dateList.get(i).get(j)).EndDate = ((bridgeAttr) dateList.get(i).get(j)).StartDate =
                                            ((String) ((JSONObject) ((JSONArray) bridgepro.get("m_girder")).get(NO)).get("m_inputdate")).replace("-", "");
                                }
                            }
                        }
                    }

                    //保存数据到本地
                    if (pd != null && teActivity != null) {
                        Message message = new Message();
                        message.what = HHANDLER_SAVEPROGRESS_LOCAL;//标志是哪个线程传数据  
                        handlerProgress.sendMessage(message);//发送message信息 
                    }
                    saveProgressDataToDatabase();

                    //渲染地图
                    if (isDownLoadOnly() == false) {
                        //显示到skyline
                        strDate = strDate.replace("-", "");
                        if (pd != null && teActivity != null) {
                            Message message = new Message();
                            message.what = HHANDLER_PROGRESS_RENDING;//标志是哪个线程传数据
                            handlerProgress.sendMessage(message);//发送message信息 
                        }
                        ModelShowOrUnSHow(strDate, false, true);
                        Message message = new Message();
                        message.what = HHANDLER_FILLPROGRESS_OVER;//标志是哪个线程传数据  
                        handlerProgress.sendMessage(message);//发送message信息 
                    }
                } catch (Exception e) {
                    Log.e("tsdi", e.getMessage());
                    Message message = new Message();
                    message.what = HHANDLER_SYS_FINISHED;//事件结束标志  
                    handlerProgress.sendMessage(message);//发送message信息
                }
            }
        });
    }

    /***
     * 保存进度信息到本地数据库
     * 说明：速度太慢，暂不使用
     * 建议：最好放到子线程去执行
     */
    public void saveProgressDataToDatabase() {
//        if (dateList == null || dateList.size() == 0)
//            return;
//        rimDB db = new rimDB(getCurrentActivityContext());
//        db.opendb(OPENDB_WRITEABLE);
//        db.execSQL1("delete from progressBridge where projectid=" + mProjectID + " and name='" + gdName + "'");
//        for (int i = 0; i < dateList.size(); i++) {
//            for (int k = 0; k < dateList.get(i).size(); k++) {
//                bridgeAttr attr = (bridgeAttr) dateList.get(i).get(k);
//                db.execSQL1("insert into  progressBridge values(null," + mProjectID + ",'" + gdName + "','" + attr.StartDate + "','" + attr.EndDate + "','" + attr.PStartDate + "','" + "','" + attr.PEndDate + attr.Type
//                        + "'," + attr.Index1 + "," + attr.Index2 + "," + attr.Index3 + "," + attr.Pos + "," + attr.Length + ")");
//            }
//        }
//        db.closedb();
    }

    /***
     * 把桥的属性数据转换成列表形式，便于循环操作
     * 说明：专用函数，不要调用
     * @param jsonAttrs 从服务器查询得到的桥属性数据
     * @param qiaoList 数组方式的桥属性数据
     */
    public void JsonToAttrs(JSONObject jsonAttrs, List<List> qiaoList) {
        try {
            JSONArray jsonArray1 = new JSONArray(jsonAttrs.getString("data"));
            List<bridgeAttr> attrList = new ArrayList<bridgeAttr>();
            for (int i = 0; i < jsonArray1.length(); i++) {
                JSONArray jsonArray = (JSONArray) jsonArray1.get(i);
                for (int j = 0; j < jsonArray.length(); j++) {
                    bridgeAttr bridgeAttr = new bridgeAttr();
                    JSONObject object = (JSONObject) jsonArray.get(j);
                    bridgeAttr.StartDate = object.getString("StartDate");
                    bridgeAttr.EndDate = object.getString("EndDate");
                    bridgeAttr.PStartDate = object.getString("PStartDate");
                    bridgeAttr.PEndDate = object.getString("PEndDate");
                    bridgeAttr.Type = object.getString("Type");
                    bridgeAttr.Index1 = object.getInt("Index1");
                    bridgeAttr.Index2 = object.getInt("Index2");
                    bridgeAttr.Index3 = object.getInt("Index3");
                    bridgeAttr.Pos = object.getInt("Pos");
                    bridgeAttr.Length = object.getInt("Length");
                    attrList.add(bridgeAttr);
                }
                qiaoList.add(attrList);
            }
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
    }

    //定义handlerMesh对象接收子线程Get3DMLLayer消息
    public Handler handlerProgress = new Handler() {
        @Override
        //当有消息发送出来的时候就执行Handler的这个方法
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Date dt = new Date();
            SimpleDateFormat dm = new SimpleDateFormat("yyyy-MM-dd");
            String strDate = dm.format(dt);
            strDate.replace("-", "");
            switch (msg.what) {
                case 0://请求失败
                    if (pd != null && teActivity != null) {
                        pd.dismiss();
                        pd = null;
                    }
                    break;
                case HHANDLER_PROGRESS_START://第一步
                    if (pd != null && teActivity != null)
                        pd.setMessage(teActivity.getString(R.string.info_app_progress_obj));
                    Get3DMLLayer(gdName);
                    break;
                case HHANDLER_GET_MESHLAYER://第二步
                    if (pd != null && teActivity != null)
                        pd.setMessage(teActivity.getString(R.string.info_app_progress_webapi));
                    //webapi请求服务器上的进度属性信息
                    objProgressTask progressTask = new BridgeProgressTask(QUERY_OBJ_ATTRS, 1, featureAttributes, gdName, strDate);
                    progressTask.getObjProgressInfo(handlerProgress);
                    //webapi请求服务器上的请求描述信息
                    objProgressTask progressTask1 = new BridgeProgressTask(QUERY_OBJ_PRO, 1, null, gdName, strDate);
                    progressTask1.getObjProgressInfo(handlerProgress);
                    break;
                case HHANDLER_SGJD_ATTRS://请求成功
                    //根据服务器端返回的jsonObject给获取对象的进度信息
                    try {
                        jsonAttrs = new JSONObject(msg.getData().getString("jsonObj"));
                        //LogInfo(jsonObject.toString(),3500);
                        String code1 = jsonAttrs.getString("code");
                        //根据code判断是否登陆成功
                        if (Integer.parseInt(code1) != 1) {
                        } else {
                            if (jsonPro != null) {
                                bInit = true;
                                if (pd != null && teActivity != null)
                                    pd.setMessage(teActivity.getString(R.string.info_app_progress_set_value));
                                FillRealProgressDatetable(jsonAttrs, jsonPro);
                            }
                        }
                    } catch (Exception e) {

                        if (pd != null && teActivity != null) {
                            pd.dismiss();
                            pd = null;
                        }
                        Log.e("tsdi", e.getMessage());
                    }
                    break;
                case HHANDLER_SGJD_PRO://请求成功
                    //根据服务器端返回的jsonObject给获取对象的进度信息
                    try {
                        jsonPro = new JSONObject(msg.getData().getString("jsonObj"));
                        //LogInfo(jsonObject.toString(),3500);
                        String code = jsonPro.getString("code");
                        //根据code判断是否登陆成功
                        if (Integer.parseInt(code) != 1) {
                        } else {
                            if (jsonAttrs != null) {
                                bInit = true;
                                if (pd != null && teActivity != null)
                                    pd.setMessage(teActivity.getString(R.string.info_app_progress_set_value));
                                FillRealProgressDatetable(jsonAttrs, jsonPro);
                            }
                        }
                    } catch (Exception e) {
                        if (pd != null && teActivity != null) {
                            pd.dismiss();
                            pd = null;
                        }
                        Log.e("tsdi", e.getMessage());
                    }
                    break;
                case HHANDLER_SAVEPROGRESS_LOCAL:
                    if (pd != null && teActivity != null) {
                        pd.setMessage(teActivity.getString(R.string.info_app_progress_save_local));
                    }
                    break;
                case HHANDLER_PROGRESS_RENDING:
                    if (pd != null && teActivity != null) {
                        pd.setMessage(teActivity.getString(R.string.info_app_progress_rending));
                    }
                    break;
                case HHANDLER_FILLPROGRESS_OVER:
                    if (pd != null && teActivity != null) {
                        pd.dismiss();
                        pd = null;
                    }
                    //Log.i("tsdi",getBridgeName(gdName));
                    flyToObjectByName(fullPathGDName);
                    break;
                case HHANDLER_SYS_FINISHED:
                    //只要执行到这里就关闭对话框
                    if (pd != null && teActivity != null) {
                        pd.dismiss();
                        pd = null;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /***
     * 根据桥的名字获取桥的全路径名字
     * 注意：需要放到子线程中去执行
     * 专用函数，项目标准化后，应该不用这个函数，目前的数据组织有点乱才用这个函数来暂时处理
     * @param name 工点名（桥名）
     * @return 全路径名字
     */
    private String getBridgeFullPathName(String name) {
        IProjectTree ProjectTree = ISGWorld.getInstance().getProjectTree();
        String itemname = "";
        itemname = "桥梁\\" + name;
        String itemid = ProjectTree.FindItem(itemname);
        if (itemid.equals("0") || itemid.length() == 0) {
            itemname = "施工进度\\" + itemname;
        }
        if (itemid.equals("0") || itemid.length() == 0) {
            itemname = "桥梁\\" + name + "_point";
        }
        return itemname;
    }
    /***
     * 根据类型和名字,从Skyline地图中得到桥的图层及属性信息
     * @param gdname 图层名(工点名（桥名)）
     */
    public void Get3DMLLayer(final String gdname) {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    //String info = ""+System.currentTimeMillis();

                    IProjectTree ProjectTree = ISGWorld.getInstance().getProjectTree();
                    //存储对象目标，一个桥或者隧道
                    IMeshLayer meshlayer = null;
//                    String itemname = "";
//                    //itemname = "桥梁\\" + gdname;
//                    itemname = "桥梁\\" + "温河特大桥_point";
//
//                    String itemid = ProjectTree.FindItem(getBridgeName(gdname));
//                    if (itemid.equals("0")||itemid.length()==0) {
//                        itemname = "施工进度\\" + itemname;
//                    }
                    fullPathGDName = getBridgeFullPathName(gdname);
                    String itemid = ProjectTree.FindItem(fullPathGDName);

                    ITerraExplorerObject object = ProjectTree.GetObject(itemid);
                    if (object.getObjectType() == ObjectTypeCode.OT_3D_MESH_LAYER) {
                        meshlayer = object.CastTo(IMeshLayer.class);
                    }
                    //获取对象目标的空间范围
                    double cVerticesArray[] = {meshlayer.getBBox().getMinX(), meshlayer.getBBox().getMinY(), 0.0,
                            meshlayer.getBBox().getMaxX(), meshlayer.getBBox().getMinY(), 0.0,
                            meshlayer.getBBox().getMaxX(), meshlayer.getBBox().getMaxY(), 0.0,
                            meshlayer.getBBox().getMinX(), meshlayer.getBBox().getMaxY(), 0.0,
                            meshlayer.getBBox().getMinX(), meshlayer.getBBox().getMinY(), 0.0};
                    ILinearRing linering = ISGWorld.getInstance().getCreator().getGeometryCreator().CreateLinearRingGeometry(cVerticesArray);
                    //对象目标的外包多边形
                    IPolygon cPolygonGeometry = ISGWorld.getInstance().getCreator().getGeometryCreator().CreatePolygonGeometry(linering, null);

                    //遍历对象
                    for (int i = 0; i < meshlayer.getFeatureLayers().getCount(); i++) {
                        List<Hashtable> tmpList = new ArrayList<Hashtable>();
                        I3DMLFeatureLayer flayer = null;
                        flayer = ((TEIUnknownHandle) meshlayer.getFeatureLayers().get_Item(i)).CastTo(I3DMLFeatureLayer.class);
                        //下面的查询用时2秒！！！！！！！！！！！！！
                        IFeatures features = flayer.ExecuteSpatialQuery(((TEIUnknownHandle) cPolygonGeometry).CastTo(IGeometry.class), 2);
                        //存储实体对象，对象为BIM分解类型的对象组（EBS）
                        listfeaturelayers.add(features);

                        //遍历对象目标的构件（分解后的组成部分）,提取实体对象的属性信息并存储，对象为BIM分解类型的对象组（EBS）
                        for (int j = 0; j < features.getCount(); j++) {
                            Hashtable<String, String> hObj = new Hashtable<String, String>();
                            IFeature feature = ((TEIUnknownHandle) features.get_Item(j)).CastTo(IFeature.class);

                            String qType = feature.getFeatureAttributes().GetFeatureAttribute("Type").getValue();
                            String index1 = feature.getFeatureAttributes().GetFeatureAttribute("Index3").getValue();
                            String index2 = feature.getFeatureAttributes().GetFeatureAttribute("Index1").getValue();
                            String index3 = feature.getFeatureAttributes().GetFeatureAttribute("Index2").getValue();
                            String pos = feature.getFeatureAttributes().GetFeatureAttribute("Pos").getValue();
                            String qLength = feature.getFeatureAttributes().GetFeatureAttribute("Length").getValue();
                            hObj.put("qType", qType);
                            hObj.put("index1", index1);
                            hObj.put("index2", index2);
                            hObj.put("index3", index3);
                            hObj.put("pos", pos);
                            hObj.put("qLength", qLength);
                            //hObj.put("qLength",qLength);
                            tmpList.add(hObj);
                        }
                        if (tmpList.size() == 0) {//获取空间信息是失败，直接退出
                            Message message = new Message();
                            message.what = HHANDLER_SYS_FINISHED;//事件结束标志  
                            handlerProgress.sendMessage(message);//发送message信息
                            return;
                        }
                        featureAttributes.add(tmpList);
                    }

                    Message message = new Message();
                    message.what = HHANDLER_GET_MESHLAYER;//获取空间对象成功的标志  
                    handlerProgress.sendMessage(message);//发送message信息 
                } catch (Exception e) {
                    Log.i("tsdi", e.getMessage());
                    Message message = new Message();
                    message.what = HHANDLER_SYS_FINISHED;//事件结束标志  
                    handlerProgress.sendMessage(message);//发送message信息
                }
            }
        });
    }

    /***
     * 把进度信息，呈现到skyline
     * 注意：需要放到子线程中去执行
     * @param checkDate 截止日期
     * @param bPlan 是否显示计划进度
     * @param bShj 是否显示实际进度
     */
    public void ModelShowOrUnSHow(final String checkDate, final boolean bPlan, final boolean bShj) {

        try {
            for (int i = 0; i < listfeaturelayers.size(); i++) {
                IFeatures features = listfeaturelayers.get(i);
                for (int k = 0; k < features.getCount(); k++) {
                    //IFeature feature = (IFeature) features.get_Item(k);
                    IFeature feature = ((TEIUnknownHandle) features.get_Item(k)).CastTo(IFeature.class);
                    feature.getID();
                    float checkdate = Float.parseFloat(checkDate);
                    float startdate;
                    float enddate;
                    if ((bPlan && !bShj) || (!bPlan && bShj)) {
                        //feature.Tint.SetAlpha(0);
                        if (bPlan) {
                            startdate = Float.parseFloat(((bridgeAttr) dateList.get(i).get(k)).PStartDate);
                            enddate = Float.parseFloat(((bridgeAttr) dateList.get(i).get(k)).PEndDate);
                        } else {
                            startdate = Float.parseFloat(((bridgeAttr) dateList.get(i).get(k)).StartDate);
                            enddate = Float.parseFloat(((bridgeAttr) dateList.get(i).get(k)).EndDate);
                        }
                        if (enddate <= checkdate) {
                            //已完成
                            //feature.Show = true;
                            feature.getTint().FromHTMLColor("#00FF00");
                            feature.getTint().SetAlpha(1);
                        } else if (checkdate >= startdate && checkdate < enddate) {
                            //正在施工中
                            //feature.Show = true;
                            feature.getTint().FromHTMLColor("#00FF00");
                            feature.getTint().SetAlpha(1);
                        } else {
                            //未开工
                            //feature.Show = false;
                            //featrue66.Tint.SetAlpha(0.0);
                        }
                    } else {
                        feature.getTint().SetAlpha(0);
                        float pstartdate = Float.parseFloat(((bridgeAttr) dateList.get(i).get(k)).PStartDate);
                        startdate = Float.parseFloat(((bridgeAttr) dateList.get(i).get(k)).StartDate);
                        if (pstartdate < startdate) {
                            if (pstartdate <= checkdate && startdate > checkdate) {
                                //feature.Show = true;
                                //feature.Tint.FromHTMLColor('#FF0000');

                                feature.getTint().FromHTMLColor("#0000FF");
                                feature.getTint().SetAlpha(1);
                                //feature.Tint.SetAlpha(0.2); //计划完成实际没完 红色警告
                            } else if (startdate <= checkdate) {
                                //feature.Show = true;

                                feature.getTint().FromHTMLColor("#00FF00");
                                feature.getTint().SetAlpha(1);

                            } else {
                                //feature.Show = false;
                            }
                        } else if (pstartdate > startdate) {
                            if (startdate <= checkdate && pstartdate > checkdate) {
                                //feature.Show = true;
                                //feature.Tint.FromHTMLColor('#0000FF');

                                feature.getTint().FromHTMLColor("#FF0000");
                                feature.getTint().SetAlpha(1);
                                //feature.Tint.SetAlpha(0.2); //实际完了 计划没完
                            } else if (pstartdate <= checkdate) {
                                //feature.Show = true;
                                feature.getTint().FromHTMLColor("#00FF00");
                                feature.getTint().SetAlpha(1);

                            } else {
                                //feature.Show = false;
                            }
                        } else {
                            if (startdate <= checkdate) {
                                //feature.Show = true;
                                feature.getTint().FromHTMLColor("#00FF00");
                                feature.getTint().SetAlpha(1);
                            } else {
                                //feature.Show = false;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
    }
}
