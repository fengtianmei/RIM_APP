package com.skyline.rim.utils;

import android.util.Log;

/**
 * 用于打印出超长的Log信息
 * Created by mft on 2017/10/26.
 */

public class LogLongInfo {

    /**
     * 分段打印出较长log文本
     *
     * @param logContent 打印文本
     * @param showLength 规定每段显示的长度（AndroidStudio控制台打印log的最大信息量大小为4k）
     */
    public static void LogInfo(String logContent, int showLength) {
        if (logContent.length() > showLength) {
            String show = logContent.substring(0, showLength);
            Log.i("tsdi", show);
            /*剩余的字符串如果大于规定显示的长度，截取剩余字符串进行递归，否则打印结果*/
            if ((logContent.length() - showLength) > showLength) {
                String partLog = logContent.substring(showLength, logContent.length());
                LogInfo(partLog, showLength);
            } else {
                String printLog = logContent.substring(showLength, logContent.length());
                Log.i("tsdi", printLog);
            }

        } else {
            Log.i("tsdi", logContent);
        }
    }
}
