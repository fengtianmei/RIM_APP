package com.skyline.rim.sqlite;


import android.content.Context;
import android.database.Cursor;

import static com.skyline.rim.globaldata.ConstData.OPENDB_READONLY;
import static com.skyline.rim.globaldata.UserInfo.mProjectID;

/**
 * Created by mft on 2017/11/2.
 */

public class rimDB extends baseDB {
    public rimDB(Context context) {
        super(context);
    }

    /***
     * 统计本项目的桥梁数量
     * @return
     */
    public int getBridgeCount(int ver) {
        int count = 0;
        try {
            opendb(OPENDB_READONLY);
            Cursor cursor = db.rawQuery("select distinct name from bridgeList where projectid=" + mProjectID + " and ver=" + ver, null);
            if (cursor == null)
                count = 0;
            count = cursor.getCount();
            closedb();
        } catch (Exception e) {
            return 0;
        }
        return count;
    }

    /***
     * 统计本项目的隧道数量
     * @return
     */
    public int getTunnelCount(int ver) {
        int count = 0;
        try {
            opendb(OPENDB_READONLY);
            Cursor cursor = db.rawQuery("select distinct name from tunnelList where projectid=" + mProjectID + " and ver=" + ver, null);
            if (cursor == null)
                count = 0;
            count = cursor.getCount();
            closedb();
        } catch (Exception e) {
            return 0;
        }
        return count;
    }

    /***
     * 插入或更新桥的详细信息
     * @param sysl
     * @param syxq
     * @param dw
     * @param zlsl
     * @param zlxq
     * @param ylsl
     * @param ylxq
     * @param jlsl
     * @param jlxq
     * @param nlsl
     * @param nlxq
     * @param klsl
     * @param klxq
     * @param zl
     * @param gdmc
     * @param yjmc
     * @param ejmc
     */
    public void updateBridge(String sysl, String syxq, String dw, String zlsl, String zlxq, String ylsl, String ylxq, String jlsl, String jlxq, String nlsl, String nlxq, String klsl, String klxq, String zl, String gdmc, String yjmc, String ejmc) {
        sysl = (sysl != null && sysl.length() > 0) ? sysl : "null";
        syxq = (syxq != null && syxq.length() > 0) ? "'" + syxq + "'" : "null";
        dw = (dw != null && dw.length() > 0) ? "'" + dw + "'" : "null";
        zlsl = (zlsl != null && zlsl.length() > 0) ? zlsl : "null";
        zlxq = (zlxq != null && zlxq.length() > 0) ? "'" + zlxq + "'" : "null";
        ylsl = (ylsl != null && ylsl.length() > 0) ? ylsl : "null";
        ylxq = (ylxq != null && ylxq.length() > 0) ? "'" + ylxq + "'" : "null";
        jlsl = (jlsl != null && jlsl.length() > 0) ? jlsl : "null";
        jlxq = (jlxq != null && jlxq.length() > 0) ? "'" + jlxq + "'" : "null";
        nlsl = (nlsl != null && nlsl.length() > 0) ? nlsl : "null";
        nlxq = (nlxq != null && nlxq.length() > 0) ? "'" + nlxq + "'" : "null";
        klsl = (klsl != null && klsl.length() > 0) ? klsl : "null";
        klxq = (klxq != null && klxq.length() > 0) ? "'" + klxq + "'" : "null";
        zl = (zl != null && zl.length() > 0) ? zl : "null";
        gdmc = (gdmc != null && gdmc.length() > 0) ? "'" + gdmc + "'" : "null";
        yjmc = (yjmc != null && yjmc.length() > 0) ? "'" + yjmc + "'" : "null";
        ejmc = (ejmc != null && ejmc.length() > 0) ? "'" + ejmc + "'" : "null";
        execSQL("delete from bridge where projectid=" + mProjectID + " and gdmc=" + gdmc + " and yjmc=" + yjmc + " and ejmc=" + ejmc + ";");
        String sql = "INSERT INTO bridge VALUES (null," + mProjectID + "," + sysl + "," + syxq + "," + dw + "," + zlsl + "," + zlxq + "," + ylsl + "," + ylxq + "," + jlsl + "," + jlxq + "," + nlsl + "," + nlxq + "," + klsl + "," + klxq + "," + zl + "," + gdmc + "," + yjmc + "," + ejmc + ");";
        execSQL(sql);
    }

    /***
     * 插入或更新隧道的详细信息
     * @param sysl
     * @param zlsl
     * @param ylsl
     * @param jlsl
     * @param nlsl
     * @param klsl
     * @param dklc
     * @param zzlc
     * @param sjcd
     * @param qslc
     * @param gdmc
     * @param xmmc
     * @param gzm
     */
    public void updateTunnel(String sysl, String zlsl, String ylsl, String jlsl, String nlsl, String klsl, String dklc, String zzlc, String sjcd, String qslc, String gdmc, String xmmc, String gzm) {
        sysl = (sysl != null && sysl.length() > 0) ? sysl : "null";
        zlsl = (zlsl != null && zlsl.length() > 0) ? zlsl : "null";
        ylsl = (ylsl != null && ylsl.length() > 0) ? ylsl : "null";
        jlsl = (jlsl != null && jlsl.length() > 0) ? jlsl : "null";
        nlsl = (nlsl != null && nlsl.length() > 0) ? nlsl : "null";
        klsl = (klsl != null && klsl.length() > 0) ? klsl : "null";
        dklc = (dklc != null && dklc.length() > 0) ? dklc : "null";
        zzlc = (zzlc != null && zzlc.length() > 0) ? zzlc : "null";
        sjcd = (sjcd != null && sjcd.length() > 0) ? sjcd : "null";
        qslc = (qslc != null && qslc.length() > 0) ? qslc : "null";
        gdmc = (gdmc != null && gdmc.length() > 0) ? "'" + gdmc + "'" : "null";
        xmmc = (xmmc != null && xmmc.length() > 0) ? "'" + xmmc + "'" : "null";
        gzm = (gzm != null && gzm.length() > 0) ? "'" + gzm + "'" : "null";
        execSQL("delete from tunnel where projectid=" + mProjectID + " and gdmc=" + gdmc + " and xmmc=" + xmmc + " and gzm=" + gzm + ";");
        String sql = "INSERT INTO tunnel VALUES (null," + mProjectID + "," + sysl + "," + zlsl + "," + ylsl + "," + jlsl + "," + nlsl + "," + klsl + "," + dklc + "," + zzlc + "," + sjcd + "," + qslc + "," + gdmc + "," + xmmc + "," + gzm + ");";
        execSQL(sql);
    }
}
