package com.skyline.rim.globaldata;

/**
 * Created by mft on 2017/7/31.
 */

public class ConstData {
    //activity启动类型（从webview里面的链接启动teactivity时，需要设置下列相关参数）
    public static final int ACTIVITY_GIS = 1;
    public static final int ACTIVITY_WEB = 2;

    //网页里各种下载附件的缓存地址
    public static final String APP_CACHE_DIRNAME = "/tsdicache"; // web缓存目录
    public static final String APP_SKYLINE_DATA_DIRNAME = "/skylinedata"; // web缓存目录

    //启动mainactivity时的父activity类型，暂时定义如下，1=skyline activity，不是1的为其它activity
    public static final int PARENT_ACTIVITY_GIS = 1;

    //Te信息树节点类型
    public static final int ITEM_GROUP = 1;
    public static final int ITEM_LAYER = 2;
    public static final int ITEM_OBJECTS = 3;
    public static final int ITEM_LOCATION = 4;
    public static final int ITEM_PRESENTATION = 5;
    public static final int ITEM_CONTOUR_MAP = 6;
    public static final int ITEM_SLOPE_MAP = 7;

    public static final int ITEM_RASTER_LAYER = 21;
    public static final int ITEM_MESH_LAYER = 22;
    public static final int ITEM_FEATURE_LAYER = 23;
    public static final int ITEM_UP = 24;

    //Te界面功能类型
    public static final int TE_FUNCTION_TYPE_BASE = 1;
    public static final int TE_FUNCTION_TYPE_VIRTUAL_CONSTRUCTION = 2;
    public static final int TE_FUNCTION_TYPE_SKILL = 3;
    public static final int TE_FUNCTION_TYPE_PROGRESS = 4;

    //TeTool功能按钮
    public static final int TE_FUNCTION_BUTTON_FULL_PROJECT = 1;
    public static final int TE_FUNCTION_BUTTON_PROJECT_TREE = 2;
    public static final int TE_FUNCTION_BUTTON_PRESENTATION_PRE = 3;
    public static final int TE_FUNCTION_BUTTON_PRESENTATION_PLAY = 4;
    public static final int TE_FUNCTION_BUTTON_PRESENTATION_NEXT = 5;
    public static final int TE_FUNCTION_BUTTON_PRESENTATION_STEPS = 6;
    public static final int TE_FUNCTION_BUTTON_PROGRESS_PLAN = 7;
    public static final int TE_FUNCTION_BUTTON_PROGRESS_REAL = 8;
    public static final int TE_FUNCTION_BUTTON_OBJECT_ATTRIBUTE = 9;
    public static final int TE_FUNCTION_BUTTON_DATE_PICKER = 10;


    //网络请求模式
    public static final int REQUEST_GET = 1;
    public static final int REQUEST_POST = 2;


    //webapi消息回传类别参数
    public static final int HHANDLER_SGJD_ATTRS = 1;
    public static final int HHANDLER_SGJD_PRO = 2;
    public static final int HHANDLER_CENTER_LINES = 3;
    public static final int HHANDLER_CENTER_LOCATE = 4;
    public static final int HHANDLER_SYS_CHECK = 5;
    public static final int HHANDLER_SYS_FINISHED = 6;
    public static final int HHANDLER_GET_MESHLAYER = 7;
    public static final int HHANDLER_PROGRESS_START = 8;
    public static final int HHANDLER_FILLPROGRESS_OVER = 9;
    public static final int HHANDLER_SAVEPROGRESS_LOCAL = 10;
    public static final int HHANDLER_PROGRESS_RENDING = 11;
    public static final int HHANDLER_CLEAR_CACHE = 12;
    public static final int HHANDLER_SERVER_STATUS_CHANGED = 13;


    //3D对象的类型
    public static final int OBJ_3D_BRIDGE = 1;
    public static final int OBJ_3D_TUNNEL = 2;
    public static final int OBJ_3D_ROAD = 3;

    //进度展示类型
    public static final int PROGRESS_SHOW_ACTUAL = 1;
    public static final int PROGRESS_SHOW_PLAN = 2;

    //进度信息查询类型
    public static final int QUERY_OBJ_ATTRS = 1;
    public static final int QUERY_OBJ_PRO = 3;

    //数据库打开模式
    public static final int OPENDB_READONLY = 1;
    public static final int OPENDB_WRITEABLE = 2;

    //三维数据存储路径
    public static final String GEODATA_PATH_DEFAULT = "default";
    public static final String GEODATA_PATH_MEMORY = "memory";
    public static final String GEODATA_PATH_SDCARD = "sdcard";


    public static final String USER_STATE_PROJECT = "com.skyline.terraexplorer.UserState.Project";
    public static final String USER_STATE_LOCATION = "com.skyline.terraexplorer.UserState.Location";
    public static final String PROJECT_PATH = "com.skyline.terraexpolrer.ProjectsTool.PROJECT_PATH";
    public final static String[][] MIME_MapTable = {
//{后缀名，MIME类型}
            {".3gp", "video/3gpp"},
            {".apk", "application/vnd.android.package-archive"},
            {".asf", "video/x-ms-asf"},
            {".avi", "video/x-msvideo"},
            {".bin", "application/octet-stream"},
            {".bmp", "image/bmp"},
            {".c", "text/plain"},
            {".class", "application/octet-stream"},
            {".conf", "text/plain"},
            {".cpp", "text/plain"},
            {".doc", "application/msword"},
            {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls", "application/vnd.ms-excel"},
            {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe", "application/octet-stream"},
            {".gif", "image/gif"},
            {".gtar", "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h", "text/plain"},
            {".htm", "text/html"},
            {".html", "text/html"},
            {".jar", "application/java-archive"},
            {".java", "text/plain"},
            {".jpeg", "image/jpeg"},
            {".jpg", "image/jpeg"},
            {".js", "application/x-JavaScript"},
            {".log", "text/plain"},
            {".m3u", "audio/x-mpegurl"},
            {".m4a", "audio/mp4a-latm"},
            {".m4b", "audio/mp4a-latm"},
            {".m4p", "audio/mp4a-latm"},
            {".m4u", "video/vnd.mpegurl"},
            {".m4v", "video/x-m4v"},
            {".mov", "video/quicktime"},
            {".mp2", "audio/x-mpeg"},
            {".mp3", "audio/x-mpeg"},
            {".mp4", "video/mp4"},
            {".mpc", "application/vnd.mpohun.certificate"},
            {".mpe", "video/mpeg"},
            {".mpeg", "video/mpeg"},
            {".mpg", "video/mpeg"},
            {".mpg4", "video/mp4"},
            {".mpga", "audio/mpeg"},
            {".msg", "application/vnd.ms-outlook"},
            {".ogg", "audio/ogg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".pps", "application/vnd.ms-powerpoint"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop", "text/plain"},
            {".rc", "text/plain"},
            {".rmvb", "audio/x-pn-realaudio"},
            {".rtf", "application/rtf"},
            {".sh", "text/plain"},
            {".tar", "application/x-tar"},
            {".tgz", "application/x-compressed"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".wmv", "audio/x-ms-wmv"},
            {".wps", "application/vnd.ms-works"},
            {".xml", "text/plain"},
            {".z", "application/x-compress"},
            {".zip", "application/x-zip-compressed"},
            {"", "*/*"}
    };
}
