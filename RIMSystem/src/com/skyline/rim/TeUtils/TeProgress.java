package com.skyline.rim.TeUtils;

import android.app.ProgressDialog;

import com.skyline.teapi.IFeatures;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 进度展示处理基类。不要直接调用此类，请调用派生类实现具体的进度信息获取
 * Created by mft on 2017/10/30.
 */

public class TeProgress {
    //工点名字
    public String gdName;
    //工点全路径名
    public String fullPathGDName;
    //实际进度/计划进度
    int showType;
    //截至如期
    String strDate;

    //工点类型
    public int objType;

    //存储空间对象的属性（从skyline中获取），用以显示进度
    public final List<List> featureAttributes = new ArrayList<List>();
    //存储空间对象实体（从skyline中获取），从这个对象中也可以提取对象的属性（featureAttributes）
    public final List<IFeatures> listfeaturelayers = new ArrayList<IFeatures>();
    //存储从服务器上查询到的属性数据
    public final List<List> dateList = new ArrayList<List>();
    public JSONObject jsonAttrs = null;
    public JSONObject jsonPro = null;
    //标记是否数据获取完成
    public boolean bInit = false;

    public boolean isDownLoadOnly() {
        return bDownLoadOnly;
    }

    public void setDownLoadOnly(boolean bDownLoadOnly) {
        this.bDownLoadOnly = bDownLoadOnly;
    }

    public boolean bDownLoadOnly = false;
    //界面临时变量
    public ProgressDialog pd;

    public void FillRealProgressDatetable(final JSONObject jsonAttrs, final JSONObject jsonPro) {
    }

    public void JsonToAttrs(JSONObject jsonAttrs, List<List> vList) {
    }


    /***
     * 根据类型和名字,从Skyline地图中得到桥或隧道的图层及属性信息
     * @param gdname 图层名(工点名（桥、隧道名)）
     */
    public void Get3DMLLayer(final String gdname) {
    }

    /***
     * 根据从服务器上获取的信息，对skyline中鬼影的对象设置显示状态
     * @param checkDate 截止日期
     * @param bPlan 是否显示计划进度
     * @param bShj 是否显示实际进度
     */
    public void ModelShowOrUnSHow(final String checkDate, final boolean bPlan, final boolean bShj) {
    }
}
