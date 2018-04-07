package com.skyline.rim.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by mft on 2017/11/1.
 */

public class rimDBHelper extends SQLiteOpenHelper {
    //必须要有构造函数
    public rimDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    // 当第一次创建数据库的时候，调用该方法
    public void onCreate(SQLiteDatabase db) {
        try {
            //创建 桥 表（id,项目ID,名字，标段名,施工单位,版本）
            String sql = "create table bridgeList(id integer primary key autoincrement,projectid integer,name varchar(100),bdname varchar(20),worker varchar(200),ver integer)";
            db.execSQL(sql);
            //创建 隧道 表（id,项目ID,名字，标段名,施工单位，版本）
            sql = "create table tunnelList(id integer primary key autoincrement,projectid integer,name varchar(100),bdname varchar(20),worker varchar(200),ver integer)";
            db.execSQL(sql);
            //创建 桥 形象进度表（id,项目ID,名字，实际开始日期，实际结束日期，计划开始日期，计划结束日期，类型，索引1，索引2，索引3，位置，长度）
            sql = "create table progressBridge(id integer primary key autoincrement,projectid integer,name nvarchar(100)" +
                    ",StartDate varchar(8),EndDate varchar(8),PStartDate varchar(8),PEndDate varchar(8)" +
                    ",Type varchar(8),Index1 integer,Index2 integer,Index3 integer,Pos real,Lendth real" +
                    ")";
            db.execSQL(sql);
            //创建 隧道 形象进度表（id,项目ID,名字，实际开始日期，实际结束日期，计划开始日期，计划结束日期，类型，里程，长度）
            sql = "create table progressTunnel(id integer primary key autoincrement,projectid integer,name nvarchar(100)" +
                    ",StartDate varchar(8),EndDate varchar(8),PStartDate varchar(8),PEndDate varchar(8)" +
                    ",Type varchar(8),LiCheng integer,Lendth real" +
                    ")";
            db.execSQL(sql);
            //创建 桥梁详细情况表
            //id,项目ID，剩余数量，剩余详情，周累数量，周累详情，月累数量，月累详情，季累数量，季累详情，年累数量，年累详情，开累数量，开累详情，总量，工点名称，一级名称，二级名称
            sql = "create table bridge(id integer primary key autoincrement,projectid integer,sysl real,syxq  text,dw varchar(20),zlsl real,zlxq text,ylsl real,ylxq text,jlsl real,jlxq text,nlsl real,nlxq text,klsl real,klxq text,zl real,gdmc nvarchar(100),yjmc nvarchar(50),ejmc nvarchar(50),bdname nvarchar(50),bdid nvarchar(10))";
            db.execSQL(sql);
            //创建 隧道详细情况表
            //id,项目ID，剩余数量，周累数量，月累数量，季累数量，年累数量，开累数量，洞口里程，终止里程，设计长度，起始里程，工点名称，项目名称，工作面
            sql = "create table tunnel(id integer primary key autoincrement,projectid integer,sysl real,zlsl real,ylsl real,jlsl real,nlsl real,klsl real,dklc real,zzlc real,sjcd real,qslc real,gdmc nvarchar(100),xmmc nvarchar(50),gzm nvarchar(50),bdname nvarchar(50),bdid nvarchar(10))";
            db.execSQL(sql);
            //日志填报表
            sql = "create table worklog(id integer primary key autoincrement,projectid integer,sqlstring text,logdate varchar(8))";
            db.execSQL(sql);
            //统计信息表，
            sql = "create table statistics(id integer primary key autoincrement,projectid integer,type integer,ct real,zj real,wj real,dt real,jz real,l real,kw real,zh real,cq real,yg real,wf real,tf real)";
            db.execSQL(sql);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
    }

    //当更新数据库的时候执行该方法
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            //创建 桥 表（id,项目ID,名字，标段ID, 施工单位）
            String sql = "drop table bridgeList";
            db.execSQL(sql);
            sql = "create table bridgeList(id integer primary key autoincrement,projectid integer,name varchar(100),bdname varchar(20),worker varchar(200),ver integer)";
            db.execSQL(sql);
            //创建 隧道 表（id,项目ID,名字，标段ID, 施工单位）
            sql = "drop table tunnelList";
            db.execSQL(sql);
            sql = "create table tunnelList(id integer primary key autoincrement,projectid integer,name varchar(100),bdname varchar(20),worker varchar(200),ver integer)";
            db.execSQL(sql);
            //创建 桥和隧道 形象进度表（id,项目ID,名字，te对象ID,开始日期，结束日期）
            sql = "drop table progressBridge";
            db.execSQL(sql);
            sql = "create table progressBridge(id integer primary key autoincrement,projectid integer,name nvarchar(100)" +
                    ",StartDate varchar(8),EndDate varchar(8),PStartDate varchar(8),PEndDate varchar(8)" +
                    ",Type varchar(8),Index1 integer,Index2 integer,Index3 integer,Pos real,Lendth real" +
                    ")";
            db.execSQL(sql);
            sql = "drop table progressTunnel";
            db.execSQL(sql);
            sql = "create table progressTunnel(id integer primary key autoincrement,projectid integer,name nvarchar(100)" +
                    ",StartDate varchar(8),EndDate varchar(8),PStartDate varchar(8),PEndDate varchar(8)" +
                    ",Type varchar(8),LiCheng integer,Lendth real" +
                    ")";
            db.execSQL(sql);
            //创建 桥梁详细情况表
            //id,项目ID，剩余数量，剩余详情，周累数量，周累详情，月累数量，月累详情，季累数量，季累详情，年累数量，年累详情，开累数量，开累详情，总量，工点名称，一级名称，二级名称
            sql = "drop table bridge";
            db.execSQL(sql);
            sql = "create table bridge(id integer primary key autoincrement,projectid integer,sysl real,syxq  text,dw varchar(20),zlsl real,zlxq text,ylsl real,ylxq text,jlsl real,jlxq text,nlsl real,nlxq text,klsl real,klxq text,zl real,gdmc nvarchar(100),yjmc nvarchar(50),ejmc nvarchar(50),bdname nvarchar(50),bdid nvarchar(10))";
            db.execSQL(sql);
            //创建 隧道详细情况表
            //id,项目ID，剩余数量，周累数量，月累数量，季累数量，年累数量，开累数量，洞口里程，终止里程，设计长度，起始里程，工点名称，项目名称，工作面
            sql = "drop table tunnel";
            db.execSQL(sql);
            sql = "create table tunnel(id integer primary key autoincrement,projectid integer,sysl real,zlsl real,ylsl real,jlsl real,nlsl real,klsl real,dklc real,zzlc real,sjcd real,qslc real,gdmc nvarchar(100),xmmc nvarchar(50),gzm nvarchar(50),bdname nvarchar(50),bdid nvarchar(10))";
            db.execSQL(sql);
            //日志填报表
            sql = "drop table worklog";
            db.execSQL(sql);
            sql = "create table worklog(id integer primary key autoincrement,projectid integer,sqlstring text,logdate varchar(8))";
            db.execSQL(sql);
            //总体进度统计表
            sql = "drop table statistics";
            db.execSQL(sql);
            sql = "create table statistics(id integer primary key autoincrement,projectid integer,type integer,ct real,zj real,wj real,dt real,jz real,l real,kw real,zh real,cq real,yg real,wf real,tf real)";
            db.execSQL(sql);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
    }
}
