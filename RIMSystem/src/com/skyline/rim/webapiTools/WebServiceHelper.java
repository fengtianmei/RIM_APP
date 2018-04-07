package com.skyline.rim.webapiTools;

import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 网络请求操作类，富足与服务器端的webapi进行交互
 * Created by mft on 2017/7/16.
 */

public class WebServiceHelper {
    /**
     * 通过get方式获取网络中的JSON数据
     *
     * @param strUrl web请求地址
     * @return
     * @throws Exception
     */
    public static JSONObject getJSONObject(String strUrl) throws Exception {
        JSONObject jsonObject = null;
        try {
            URL url = new URL(strUrl);
            // 利用HttpURLConnection对象，我们可以从网页中获取网页数据
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 单位为毫秒，设置超时时间为5秒
            conn.setConnectTimeout(15 * 1000);
            // HttpURLConnection对象是通过HTTP协议请求path路径的，所以需要设置请求方式，可以不设置，因为默认为get
            conn.setRequestMethod("GET");
            // 请求头必须设置，参数必须正确。  
            conn.setRequestProperty("Accept", "application/json,text/html;q=0.9,image/webp,*/*;q=0.8");
            conn.setRequestProperty("Cache-Control", "max-age=0");
            //conn.setRequestProperty("Content-Length", String.valueOf(strUrl.length()));
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.connect();
            if (conn.getResponseCode() == 200) {// 判断请求码是否200，否则为失败
                InputStream is = conn.getInputStream(); // 获取输入流
                byte[] data = readStream(is); // 把输入流转换成字符串组
                String json = new String(data); // 把字符串组转换成字符串
                // 数据形式：{"total":2,"success":true,"arrayData":[{"id":1,"name":"张三"},{"id":2,"name":"李斯"}]}
                jsonObject = new JSONObject(json); // 返回的数据形式是一个Object类型，所以可以直接转换成一个Object
            }
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
        return jsonObject;
    }

    /**
     * 通过post方式获取网络中的JSON数据
     *
     * @param strUrl web请求地址
     * @param obj    请求的参数
     * @return
     * @throws Exception
     */
    public static JSONObject postJSONObject(String strUrl, JSONObject obj) throws Exception {
        JSONObject jsonObject = null;
        try {
            URL url = new URL(strUrl);
            // 建立http连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 设置允许输出
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 设置不用缓存
            conn.setUseCaches(false);
            // 设置传递方式
            conn.setRequestMethod("POST");
            // 设置维持长连接
            conn.setRequestProperty("Connection", "Keep-Alive");
            // 设置文件字符集:
            conn.setRequestProperty("Charset", "UTF-8");
            //转换为字节数组
            byte[] data = (obj.toString()).getBytes();

            // 设置文件长度
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));
            // 设置文件类型:
            conn.setRequestProperty("content-Type", "application/json");
            // 开始连接请求
            conn.connect();
            OutputStream out = conn.getOutputStream();

            // 写入请求的字符串
            out.write((obj.toString()).getBytes());
            out.flush();
            out.close();
            if (conn.getResponseCode() == 200) {// 判断请求码是否200，否则为失败
                InputStream is = conn.getInputStream(); // 获取输入流
                byte[] outdata = readStream(is); // 把输入流转换成字符串组
                String json = new String(outdata); // 把字符串组转换成字符串
                jsonObject = new JSONObject(json); // 返回的数据形式是一个Object类型，所以可以直接转换成一个Object
            }
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
        return jsonObject;
    }

    private static byte[] readStream(InputStream inputStream) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            bout.write(buffer, 0, len);
        }
        bout.close();
        inputStream.close();
        return bout.toByteArray();
    }
}
