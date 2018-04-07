package com.skyline.rim.TeUtils;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.skyline.rim.dataStruct.tunnelAttr;
import com.skyline.rim.sqlite.rimDB;
import com.skyline.rim.webapiTools.objProgressTask;
import com.skyline.rim.webapiTools.tunnelProgressTask;
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

import java.util.ArrayList;
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
import static com.skyline.rim.globaldata.ConstData.OBJ_3D_TUNNEL;
import static com.skyline.rim.globaldata.ConstData.OPENDB_WRITEABLE;
import static com.skyline.rim.globaldata.ConstData.QUERY_OBJ_ATTRS;
import static com.skyline.rim.globaldata.ConstData.QUERY_OBJ_PRO;
import static com.skyline.rim.globaldata.UserInfo.mProjectID;
import static com.skyline.rim.globaldata.systemInfo.teActivity;
import static com.skyline.terraexplorer.TEApp.getCurrentActivityContext;

/**
 * Created by mft on 2017/10/30.
 */

public class TeTunnelProgress extends TeProgress {
    /***
     * 获取目标隧道的进度信息并显示出来。步骤：1.获取空间信息Get3DMLLayer-->2.请求服务器端信息tunnelProgressTask--->3.把服务器端的信息叠加到skyling对象，并进行显示
     * @param objName 对象名字
     * @param showType 进度类型（实际、计划）PROGRESS_SHOW_ACTUAL/PROGRESS_SHOW_PLAN
     * @param strDate 统计时间
     */
    public void show(String objName, int showType, String strDate) {
        this.setDownLoadOnly(false);
        gdName = objName;
        this.showType = showType;
        this.strDate = strDate;
        objType = OBJ_3D_TUNNEL;
        if (teActivity != null)
            pd = ProgressDialog.show(teActivity, teActivity.getString(R.string.info_app_progress_title), teActivity.getString(R.string.info_app_progress_ing));
        Message message = new Message();
        message.what = HHANDLER_PROGRESS_START;//标志是哪个线程传数据  
        handlerProgress.sendMessage(message);//发送message信息 
    }

    /***
     * 暂时不用这个函数
     * @param objName
     * @param showType
     * @param strDate
     */
    public void downLoadProgress(String objName, int showType, String strDate) {
        this.setDownLoadOnly(true);
        gdName = objName;
        this.showType = showType;
        this.strDate = strDate;
        objType = OBJ_3D_TUNNEL;
        Message message = new Message();
        message.what = HHANDLER_PROGRESS_START;//标志是哪个线程传数据  
        handlerProgress.sendMessage(message);//发送message信息 
    }

    //定义handlerMesh对象接收子线程Get3DMLLayer消息
    public Handler handlerProgress = new Handler() {
        @Override
        //当有消息发送出来的时候就执行Handler的这个方法
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
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
                case HHANDLER_GET_MESHLAYER:
                    if (pd != null && teActivity != null)
                        pd.setMessage(teActivity.getString(R.string.info_app_progress_webapi));
                    objProgressTask progressTask = new tunnelProgressTask(QUERY_OBJ_ATTRS, 1, featureAttributes, gdName, strDate);
                    progressTask.getObjProgressInfo(handlerProgress);
                    //webapi请求服务器上的请求描述信息
                    objProgressTask progressTask1 = new tunnelProgressTask(QUERY_OBJ_PRO, 1, null, gdName, strDate);
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
                    flyToObjectByName("隧道\\" + gdName);
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
     * 根据类型和名字,从Skyline地图中得到隧道的图层及属性信息
     * @param gdname 图层名(工点名（隧道名)）
     */
    public void Get3DMLLayer(final String gdname) {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    IProjectTree ProjectTree = ISGWorld.getInstance().getProjectTree();
                    //存储对象目标，一个桥或者隧道
                    IMeshLayer meshlayer = null;
                    String itemname = "";
                    itemname = "隧道\\" + gdname;
                    String itemid = ProjectTree.FindItem(itemname);
                    if (itemid.equals("0")) {
                        itemname = "施工进度\\" + itemname;
                    }
                    itemid = ProjectTree.FindItem(itemname);

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
                        IFeatures features = flayer.ExecuteSpatialQuery(((TEIUnknownHandle) cPolygonGeometry).CastTo(IGeometry.class), 2);
                        //存储实体对象，对象为BIM分解类型的对象组（EBS）
                        listfeaturelayers.add(features);

                        //遍历对象目标的构件（分解后的组成部分）,提取实体对象的属性信息并存储，对象为BIM分解类型的对象组（EBS）
                        for (int j = 0; j < features.getCount(); j++) {
                            Hashtable<String, String> hObj = new Hashtable<String, String>();
                            IFeature feature = ((TEIUnknownHandle) features.get_Item(j)).CastTo(IFeature.class);
                            String lc = feature.getFeatureAttributes().GetFeatureAttribute("LiCheng").getValue();
                            String mtype = feature.getFeatureAttributes().GetFeatureAttribute("Type").getValue();
                            String mlen = feature.getFeatureAttributes().GetFeatureAttribute("Length").getValue();
                            hObj.put("lc", lc);
                            hObj.put("mtype", mtype);
                            hObj.put("mlen", mlen);
                            tmpList.add(hObj);
                        }
                        featureAttributes.add(tmpList);
                    }
                    Message message = new Message();
                    message.what = HHANDLER_GET_MESHLAYER;//标志是哪个线程传数据  
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
     * 把隧道的属性数据转换成列表形式，便于循环操作
     * @param jsonAttrs
     * @param tunnelList
     */
    public void JsonToAttrs(JSONObject jsonAttrs, List<List> tunnelList) {
        try {
            JSONArray jsonArray1 = new JSONArray(jsonAttrs.getString("data"));
            List<tunnelAttr> attrList = new ArrayList<tunnelAttr>();
            for (int i = 0; i < jsonArray1.length(); i++) {
                JSONArray jsonArray = (JSONArray) jsonArray1.get(i);
                for (int j = 0; j < jsonArray.length(); j++) {
                    tunnelAttr tunnelA = new tunnelAttr();
                    JSONObject object = (JSONObject) jsonArray.get(j);
                    tunnelA.StartDate = object.getString("StartDate");
                    tunnelA.EndDate = object.getString("EndDate");
                    tunnelA.PStartDate = object.getString("PStartDate");
                    tunnelA.PEndDate = object.getString("PEndDate");
                    tunnelA.Type = object.getString("Type");
                    tunnelA.LiCheng = object.getInt("LiCheng");
                    tunnelA.Length = object.getDouble("Length");
                    attrList.add(tunnelA);
                }
                tunnelList.add(attrList);
            }
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
    }

    /***
     * 根据进度信息填充skyling进度数据并显示到skyline
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
                    JSONObject tunnelpro = new JSONObject(jsonPro.getString("data"));
                    for (int i = 0; i < featureAttributes.size(); i++) {
                        //逐行获取属性值
                        for (int j = 0; j < featureAttributes.get(i).size(); j++) {
                            int mtype = Integer.parseInt(((Hashtable<String, String>) featureAttributes.get(i).get(j)).get("mtype"));
                            float km = Float.parseFloat(((Hashtable<String, String>) featureAttributes.get(i).get(j)).get("lc"));
                            switch (mtype) {
                                case 1: // 初支
                                    for (int k = 0; k < ((JSONArray) tunnelpro.get("m_seg")).length(); k++) {
                                        if ((Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_seg")).get(k))).get("m_kwfinskm").toString()) - km) * (Float.parseFloat(((JSONObject) ((JSONArray) tunnelpro.get("m_seg")).get(k)).get("m_kwfinekm").toString()) - km) <= 0) {
                                            ((tunnelAttr) dateList.get(i).get(j)).EndDate = ((tunnelAttr) dateList.get(i).get(j)).StartDate = ((JSONObject) ((JSONArray) tunnelpro.get("m_seg")).get(k)).get("m_inputdate").toString().replace("-", "");
                                            break;
                                        }
                                    }
                                    break;
                                case 2: //衬砌
                                    for (int k = 0; k < ((JSONArray) tunnelpro.get("m_seg")).length(); k++) {

                                        if ((Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_seg")).get(k))).get("m_cqfinekm").toString()) - km) * (Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_seg")).get(k))).get("m_cqfinskm").toString()) - km) <= 0) {
                                            ((tunnelAttr) dateList.get(i).get(j)).EndDate = ((tunnelAttr) dateList.get(i).get(j)).StartDate = ((JSONObject) ((JSONArray) tunnelpro.get("m_seg")).get(k)).get("m_inputdate").toString().replace("-", "");
                                            break;
                                        }
                                    }
                                    break;
                                case 3: //仰拱
                                    for (int k = 0; k < ((JSONArray) tunnelpro.get("m_seg")).length(); k++) {
                                        if ((Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_seg")).get(k))).get("m_ygfinekm").toString()) - km) * (Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_seg")).get(k))).get("m_ygfinskm").toString()) - km) <= 0) {
                                            ((tunnelAttr) dateList.get(i).get(j)).EndDate = ((tunnelAttr) dateList.get(i).get(j)).StartDate = ((JSONObject) ((JSONArray) tunnelpro.get("m_seg")).get(k)).get("m_inputdate").toString().replace("-", "");
                                            break;
                                        }
                                    }
                                    break;
                                case 4: //仰拱
                                    for (int k = 0; k < ((JSONArray) tunnelpro.get("m_seg")).length(); k++) {
                                        if ((Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_seg")).get(k))).get("m_ygfinekm").toString()) - km) * (Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_seg")).get(k))).get("m_ygfinskm").toString()) - km) <= 0) {
                                            ((tunnelAttr) dateList.get(i).get(j)).EndDate = ((tunnelAttr) dateList.get(i).get(j)).StartDate = ((JSONObject) ((JSONArray) tunnelpro.get("m_seg")).get(k)).get("m_inputdate").toString().replace("-", "");
                                            break;
                                        }
                                    }
                                    break;
                                case 5: //沟槽
                                    for (int k = 0; k < ((JSONArray) tunnelpro.get("m_groove")).length(); k++) {
                                        if ((Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_groove")).get(k))).get("m_finekm").toString()) - km) * (Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_groove")).get(k))).get("m_finskm").toString()) - km) <= 0) {
                                            ((tunnelAttr) dateList.get(i).get(j)).EndDate = ((tunnelAttr) dateList.get(i).get(j)).StartDate = ((JSONObject) ((JSONArray) tunnelpro.get("m_groove")).get(k)).get("m_inputdate").toString().replace("-", "");
                                            break;
                                        }
                                    }
                                    break;
                                case 6: //洞室隧衬砌
                                    //for (int k = 0; k < tunnelpro.m_seg.Count; k++) {
                                    for (int k = 0; k < ((JSONArray) tunnelpro.get("m_seg")).length(); k++) {
                                        if ((Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_seg")).get(k))).get("m_cqfinekm").toString()) - km) * (Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_seg")).get(k))).get("m_cqfinskm").toString()) - km) <= 0) {
                                            ((tunnelAttr) dateList.get(i).get(j)).EndDate = ((tunnelAttr) dateList.get(i).get(j)).StartDate = ((JSONObject) ((JSONArray) tunnelpro.get("m_seg")).get(k)).get("m_inputdate").toString().replace("-", "");
                                            break;
                                        }
                                    }
                                    break;
                                default:
                                    if (mtype < 0) {
                                        int xjno = -1 * mtype - 1;
                                        if (((Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_well")).get(xjno))).get("m_finekm").toString()) - km) * ((Float.parseFloat(((JSONObject) (((JSONArray) tunnelpro.get("m_well")).get(xjno))).get("m_finskm").toString()) - km)) <= 0)) {
                                            ((tunnelAttr) dateList.get(i).get(j)).EndDate = ((tunnelAttr) dateList.get(i).get(j)).StartDate = ((JSONObject) ((JSONArray) tunnelpro.get("m_well")).get(xjno)).get("m_inputdate").toString().replace("-", "");
                                        }
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
//                        if (pd != null && teActivity != null)
//                            pd.setMessage(teActivity.getString(R.string.info_app_progress_rending));
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
     */
    public void saveProgressDataToDatabase() {
//        if (dateList == null || dateList.size() == 0)
//            return;
//        rimDB db = new rimDB(getCurrentActivityContext());
//        db.opendb(OPENDB_WRITEABLE);
//        db.execSQL1("delete from progress_tunnel where projectid=" + mProjectID + " and name='" + gdName + "'");
//        for (int i = 0; i < dateList.size(); i++) {
//            for (int k = 0; k < dateList.get(i).size(); k++) {
//                tunnelAttr attr = (tunnelAttr) dateList.get(i).get(k);
//                db.execSQL1("insert into  progress_tunnel values(null," + mProjectID + ",'" + gdName + "','" + attr.StartDate + "','" + attr.EndDate + "','" + attr.PStartDate + "','" + "','" + attr.PEndDate + attr.Type
//                        + "'," + attr.LiCheng + "," + attr.Length + ")");
//            }
//        }
//        db.closedb();
    }

    /***
     * 把进度信息，呈现到skyline
     * 注意：需要在子线程中执行
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
                    float checkdate = Float.parseFloat(checkDate);
                    float startdate;
                    float enddate;
                    if ((bPlan && !bShj) || (!bPlan && bShj)) {
                        //feature.Tint.SetAlpha(0);
                        if (bPlan) {
                            startdate = Float.parseFloat(((tunnelAttr) dateList.get(i).get(k)).PStartDate);
                            enddate = Float.parseFloat(((tunnelAttr) dateList.get(i).get(k)).PEndDate);
                        } else {
                            startdate = Float.parseFloat(((tunnelAttr) dateList.get(i).get(k)).StartDate);
                            enddate = Float.parseFloat(((tunnelAttr) dateList.get(i).get(k)).EndDate);
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

                        float pstartdate = Float.parseFloat(((tunnelAttr) dateList.get(i).get(k)).PStartDate);
                        startdate = Float.parseFloat(((tunnelAttr) dateList.get(i).get(k)).StartDate);
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
