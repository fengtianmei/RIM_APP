package com.skyline.rim.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.skyline.rim.globaldata.ConstData.GEODATA_PATH_DEFAULT;
import static com.skyline.rim.globaldata.ConstData.GEODATA_PATH_MEMORY;
import static com.skyline.rim.globaldata.ConstData.GEODATA_PATH_SDCARD;
import static com.skyline.rim.utils.FileManager.getMemoryDataPath;
import static com.skyline.rim.utils.FileManager.getSDDataPath;
import static com.skyline.rim.utils.FileManager.isExist;
import static com.skyline.rim.utils.localStorageTool.getLocalStorageVar;

/**
 * Created by mft on 2017/11/23.
 */

public class StringUtil {
    /***
     * 从字符串中提取IP地址
     * @param res
     * @return
     */
    public static String[] getIps(String res) {
        if (!TextUtils.isEmpty(res)) {
            Matcher m = Pattern.compile("((\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\:\\d{1,5})").matcher(res);
//            while (m.find()) {
//                Log.e("This" , "ip:" + m.group(1));
//            }
        }
        return null;
    }

    public static String convertGeoDataURL(Context context, String url) {
        String path = getSDDataPath(context);
        path = path + url.substring(url.lastIndexOf("/"));
        if (isExist(path))
            return path;
        path = getMemoryDataPath(context);
        path = path + url.substring(url.lastIndexOf("/"));
        if (isExist(path))
            return path;
        return url;
    }
}
