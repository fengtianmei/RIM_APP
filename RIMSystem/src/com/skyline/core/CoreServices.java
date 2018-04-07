package com.skyline.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.skyline.terraexplorer.models.UI;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;


public class CoreServices implements ComponentCallbacks2, LocationListener {

    private static final Path Path = null;
    public static Context appContext = null;
    public static Activity coreActivity = null;
    public static CoreServices coreServices = null;
    public static int nMessageBoxResult = -1;
    private static Runnable messageBoxRunnable = null;
    private static AlertDialog messageBoxDialog = null;
    private static JexlEngine jexl = null;

    // native functions
    public native int coreIsDebugMode();

    public native int coreLowMemory();

    public native void coreTrimMemory(int level);


    private static Canvas m_DrawLineCanvas = null;
    private static Paint m_DrawLinePaint = new Paint();

    private static Canvas m_DrawPathCanvas = null;
    private static Paint m_DrawPathPaint = new Paint();

    private static Path m_Path = new Path();

    private static TextPaint m_TextPaint = new TextPaint();

    private static Canvas m_BitBltCanvas = new Canvas();

    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
    // Private fields, in use by the JNI system (see sl_jnisrv.h in the Core)
    // ----------------------------------------------------------------------


    public static String java_GetApplicationName() {
        return coreActivity.getPackageName();
    }

    private static boolean java_is_network_connected() {
        ConnectivityManager cm = (ConnectivityManager) (appContext.getSystemService(Context.CONNECTIVITY_SERVICE));
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }


    // Public functions, should be called from C++ (see sl_jnisrv.h file)
    // ------------------------------------------------------------------

    public static void Init(Activity o) {

        try {
            if (coreActivity == null) {
                coreActivity = o;
                appContext = coreActivity.getApplicationContext();
            }

            if (coreServices == null) {
                coreServices = new CoreServices();
                appContext.registerComponentCallbacks(coreServices);
            }

            if (jexl == null) {
                jexl = new JexlEngine();
                jexl.setSilent(true);
                jexl.setLenient(true);
            }

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        } catch (Exception e) {
            Log.i("tsdi", "error" + e.getMessage());
        }
    }

    public static void Uninit() {
        if (coreServices != null)
            appContext.unregisterComponentCallbacks(coreServices);

        coreServices = null;
        appContext = null;
        coreActivity = null;
    }

    public static AssetManager java_GetAssetManager() {
        return appContext.getAssets();
    }

    public static int java_MessageBox(String text, String caption, int type) {
        // Types
        final int MB_OK = 0x0;
        //final int MB_OKCANCEL 	= 0x1;
        //final int MB_ABORTRETRYIGNORE = 0x2;
        //final int MB_YESNOCANCEL = 0x3;
        //final int MB_YESNO 		= 0x4;

        // Return values
        final int IDOK = 1;
        //final int IDCANCEL      = 2;
        //final int IDABORT       = 3;
        //final int IDRETRY       = 4;
        //final int IDIGNORE      = 5;
        //final int IDYES         = 6;
        //final int IDNO          = 7;

        if (type != MB_OK)
            return IDOK;    // Currently we only support MB_OK


        final String strText = text;
        final String strCaption = caption;

        nMessageBoxResult = IDOK;

        messageBoxRunnable = new Runnable() {

            @Override
            public void run() {

                messageBoxDialog = new AlertDialog.Builder(coreActivity).create();
                messageBoxDialog.setCancelable(false);
                messageBoxDialog.setTitle(strCaption);
                messageBoxDialog.setMessage(strText);
                messageBoxDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        nMessageBoxResult = IDOK;
                        synchronized (messageBoxRunnable) {
                            messageBoxRunnable.notify();
                        }
                    }
                });

                messageBoxDialog.show();
            }
        };

        coreActivity.runOnUiThread(messageBoxRunnable);

        synchronized (messageBoxRunnable) {
            try {
                messageBoxRunnable.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        messageBoxRunnable = null;
        messageBoxDialog = null;

        return nMessageBoxResult;
    }

    public static void onPause() {

        if (messageBoxRunnable == null)
            return;

        synchronized (messageBoxRunnable) {
            if (messageBoxDialog == null)
                return;

            if (messageBoxDialog.isShowing()) {
                messageBoxDialog.dismiss();
                messageBoxRunnable.notify();
            }
        }
    }

    // file system
    public static String java_getExternalStorageDirectory() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        return path;
    }

    public static String java_getDataDirectory() {
        String path = Environment.getDataDirectory().getAbsolutePath();
        return path;
    }

    public static int java_mkdir(String path) {
        int retval = 0;
        int i_name = path.lastIndexOf(File.separator);

        if (i_name <= 0)
            return -1;

        String name = path.substring(i_name + 1);
        String dir = path.substring(0, i_name);

        File f_chk = new File(path);
        if (f_chk.exists() == false)
            retval = CoreServices.java_mkdir(dir);
        else
            return 0;

        if (retval < 0)
            return -1;

        File f_dir = new File(dir, name);
        if (f_dir.mkdir() == true)
            return 0;
        else
            return -1;
    }

    // registry
    public static long java_sharedSetValue(String key, byte[] val, int type, int val_size) {
        String appName = CoreServices.java_GetApplicationName();

        SharedPreferences settings = coreActivity.getSharedPreferences(appName, android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        switch (type) {
            case 0x00000000: // REG_NONE
                break;

            case 0x00000100: // REG_QWORD
            case 0x00000004: // REG_DWORD
            case 0x00000002: // REG_BINARY
            case 0x00000400: // REG_SZ
            case 0x00000020: // REG_EXPAND_SZ
                try {
                    editor.putString(key + "_value", new String(val, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;

            case 0x00000008: // REG_DWORD_LITTLE_ENDIAN
            case 0x00000010: // REG_DWORD_BIG_ENDIAN
            case 0x00000040: // REG_LINK
            case 0x00000080: // REG_MULTI_SZ
            case 0x00000200: // REG_QWORD_LITTLE_ENDIAN
            default:
                editor.putString(key + "_value", new String(""));
                type = -1;
                val_size = 0;
        }

        editor.putInt(key + "_type", type);
        editor.putInt(key + "_size", val_size);
        editor.commit();

        return 0;
    }

    public static long java_sharedGetValue(String key, byte[] val, int[] param) {
        boolean b;

        String appName = CoreServices.java_GetApplicationName();
        SharedPreferences settings = coreActivity.getSharedPreferences(appName, android.content.Context.MODE_PRIVATE);

//    	Map<String, ?> o = settings.getAll();
        try {
            Integer t = settings.getInt(key + "_type", -1);
            Integer k = settings.getInt(key + "_size", 0);


            int type = t.intValue();
            int size = k.intValue();

            String s;
            int i;

            switch (type) {

                case 0x00000000: // REG_NONE
                    s = "";
                    break;

                case 0x00000100: // REG_QWORD
                case 0x00000004: // REG_DWORD
                case 0x00000002: // REG_BINARY
                case 0x00000400: // REG_SZ
                case 0x00000020: // REG_EXPAND_SZ
                    s = settings.getString(key + "_value", "");
                    byte bytes[] = s.getBytes();
                    for (i = 0; i < bytes.length; i++)
                        val[i] = bytes[i];
                    val[i] = 0;
                    break;

                case 0x00000008: // REG_DWORD_LITTLE_ENDIAN
                case 0x00000010: // REG_DWORD_BIG_ENDIAN
                case 0x00000040: // REG_LINK
                case 0x00000080: // REG_MULTI_SZ
                case 0x00000200: // REG_QWORD_LITTLE_ENDIAN
                default:
                    param[0] = 0; // val type
                    param[1] = 0; // val size
                    return -1L;
            }

            param[0] = type; // val type
            param[1] = size; // val size
            return 0L;
        } catch (Exception e) {
            return -1L;
        }
    }

    // networking

    public static URLConnection java_connect(String sURL, String sRequest, boolean bReload) {
        URLConnection conn = null;

        if ((sURL == null) || (sURL.isEmpty() == true))
            return null;

        try {
            if (java_is_network_connected()) {
                String urlEncoded = Uri.encode(sURL, ALLOWED_URI_CHARS);
                URL url = new URL(urlEncoded);

                conn = url.openConnection();
                conn.setUseCaches(!bReload);
                conn.setRequestProperty("Accept-Encoding", "utf8");

                if (!sRequest.isEmpty()) {
                    conn.setRequestProperty("Content-Type", "text/xml");
                    HttpURLConnection conn1 = (HttpURLConnection) conn;
                    conn1.setRequestMethod("POST");

                    conn.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    byte[] data = sRequest.getBytes("UTF-8");
                    wr.write(data);
                    wr.flush();
                    wr.close();
                }
            }
        } catch (Exception e) {
            conn = null;
            e.printStackTrace();
        }

        return conn;
    }

    public static int java_setReadTimeout(URLConnection conn, int ms) {
        conn.setReadTimeout(ms);
        return 0;
    }

    public static BufferedReader java_openReader(URLConnection con) {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        } catch (IOException e) {
            reader = null;
            e.printStackTrace();
        }

        return reader;
    }

    public static void java_closeReader(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException e) {
            reader = null;
            e.printStackTrace();
        }
    }

    public static BufferedWriter java_openWriter(URLConnection con) {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return writer;
    }

    public static void java_closeWriter(BufferedWriter writer) {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long java_getContentLength(URLConnection conn) {
        long len = (long) (conn.getContentLength());
        return len;
    }

    public static int java_read(URLConnection conn, byte[] buffer, int bytes_to_read) {
        int i = 0;
        int bytes_read = 0;

        try {
            if (java_is_network_connected()) {
                bytes_read = conn.getInputStream().read(buffer, 0, bytes_to_read); // DQ_OK
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            bytes_read = -11; // DQ_GENERIC_ERROR
            e.printStackTrace();
        } catch (IOException e) {
            bytes_read = -11; // DQ_GENERIC_ERROR
            e.printStackTrace();
        }

        if (bytes_read == -1)
            return 0; // DQ_NO_DATA

        return bytes_read;

    }

    public static int java_write(URLConnection conn, byte[] buffer, int bytes_to_write) {
        int bytes_written = bytes_to_write;

        try {
            conn.getOutputStream().write(buffer, 0, bytes_to_write);
        } catch (IOException e) {
            bytes_written = -11; // DQ_GENERIC_ERROR
            e.printStackTrace();
        }

        return bytes_written;
    }

    public static long java_getTotalMemory() {
        long totalSize = 0L;

        totalSize = getTotalMemory();

	    /*
        try {
	        Runtime info = Runtime.getRuntime();
	        totalSize = info.totalMemory();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    */

        return totalSize;
    }

    public static long java_getMaximumMemory() {
        long maxSize = 0L;

        try {
            Runtime info = Runtime.getRuntime();
            maxSize = info.maxMemory();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return maxSize;
    }

    public static long java_getAvailableMemory() {
        long freeSize = 0L;

        try {
            Runtime info = Runtime.getRuntime();
            freeSize = info.freeMemory();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return freeSize;
    }

    public static long java_getUsedMemory() {
        long totalSize = 0L;
        long freeSize = 0L;
        long usedSize = 0L;

        try {
            Runtime info = Runtime.getRuntime();
            freeSize = info.freeMemory();
            totalSize = info.totalMemory();
            usedSize = totalSize - freeSize;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return usedSize;
    }

//	public static long java_getAvailableMemory() {
//		 MemoryInfo mi = new MemoryInfo();
//		 ActivityManager activityManager = (ActivityManager)(coreActivity.getSystemService(Context.ACTIVITY_SERVICE));
//		 
//		 activityManager.getMemoryInfo(mi);
////		 long availableMegs = mi.availMem / 1048576L;
//		 
//		 return mi.availMem;
//	}

    public static void java_gc() {
        System.gc();
    }

    // Interface functions of CoreService Debugging
    // --------------------------------------------
    public static boolean IsDebugMode() {
        return (coreServices.coreIsDebugMode() != 0);
    }

    // Interface functions of ComponentCallbacks2
    // ------------------------------------------
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int level) {
    }


    // Interface functions of LocationListenr
    // --------------------------------------
    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


    // Device Context functions
    // ------------------------
    public static int java_DrawBitmap(CoreSurfaceView view, Bitmap bmp, int x, int y) {
        Canvas canvas = null;
        Rect bounds = new Rect();

        if (view == null)
            return -1;

        bounds.set(0, 0, bmp.getWidth(), bmp.getHeight());

        canvas = view.getHolder().lockCanvas();
        if (canvas != null) {
            canvas.clipRect(bounds.left + x, bounds.top + y, bounds.right + x, bounds.bottom + y, Op.REPLACE);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(bmp, x, y, view.paint);
            view.getHolder().unlockCanvasAndPost(canvas);
        }

        canvas = view.getHolder().lockCanvas();
        if (canvas != null) {
            canvas.clipRect(bounds.left + x, bounds.top + y, bounds.right + x, bounds.bottom + y, Op.REPLACE);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(bmp, x, y, view.paint);
            view.getHolder().unlockCanvasAndPost(canvas);
            return 0;
        }

        return -1;
    }

    public static CoreSurfaceView java_GetSurfaceView(int deviceID) {

        CoreSurfaceView sview = null;
        /*
		if (deviceID == 0)
			sview = (CoreSurfaceView)coreServices.coreActivity.findViewById(R.id.screendc);
		else
			sview = (CoreSurfaceView)coreServices.coreActivity.findViewById(deviceID);
		*/

        return sview;
    }

    public static int java_getDPI() {
        DisplayMetrics dm = appContext.getResources().getDisplayMetrics();
        return dm.densityDpi;
    }

    public static float java_getDPIFactor() {
        DisplayMetrics dm = appContext.getResources().getDisplayMetrics();

        float largeScreenSize = Math.max(dm.widthPixels, dm.heightPixels);// * dm.density;

        float fDPIFactor = largeScreenSize / 1024.0f;

        return fDPIFactor;

    }


    public static Object java_EvalExpr(String str) {
        final JexlContext jexlContext = new MapContext();
        Expression e = jexl.createExpression(str);
        Object o = e.evaluate(jexlContext);
        return o;

    }

    public static Paint java_CreatePaint(String familyName, boolean bItalic, boolean bUnderLine, boolean bBold, int win32lfHeight) {
        int nStyle = Typeface.NORMAL;
        if (bItalic && bBold)
            nStyle = Typeface.BOLD_ITALIC;
        else if (bItalic)
            nStyle = Typeface.ITALIC;
        else if (bBold)
            nStyle = Typeface.BOLD;


        Typeface tf = Typeface.create(familyName, nStyle);
        Paint paint = new Paint();
        paint.setTypeface(tf);

        float lfHeight = win32lfHeight;
        if (lfHeight < 0) {
            float fontLogicalSize = -lfHeight * 72.0f / 96.0f; // covert to logical font size. On Windows, the DPI is always 96 and font sizes based on it are saved to the fly file so we need the same number here in order to do a reverse translation.
            float factorSoThatIOSfontsWillLookTheSameAsInWindows = 1.5f;                    //
            lfHeight = fontLogicalSize * factorSoThatIOSfontsWillLookTheSameAsInWindows;
            //lfHeight = lfHeight /  appContext.getResources().getDisplayMetrics().density;
        }


        paint.setUnderlineText(bUnderLine);
        paint.setTextSize(lfHeight);

        return paint;
    }

    public static int java_getTextHeight(Paint paint, String text, boolean bIncludepad) {

        m_TextPaint.set(paint);
        int nWidth = 1000000;//(int)StaticLayout.getDesiredWidth(text, textPaint)
        StaticLayout layout = new StaticLayout(text, m_TextPaint, nWidth, Alignment.ALIGN_NORMAL, 1.0f, 0, bIncludepad);
        //int j = layout.getLineCount();
        int height = layout.getHeight();
        if (bIncludepad)
            height += 1;
        return height;
		/*
		Rect bounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		return -bounds.top;//bounds.offset(0, -bounds.top);
		*/
    }

    public static void java_DrawLine(Bitmap bmpDest, float x1, float y1, float x2, float y2, float width, int color) {
        if (m_DrawLineCanvas == null)
            m_DrawLineCanvas = new Canvas(bmpDest);
        else
            m_DrawLineCanvas.setBitmap(bmpDest);

        m_DrawLinePaint.setStyle(Paint.Style.STROKE);
        m_DrawLinePaint.setColor(color);
        m_DrawLinePaint.setStrokeWidth(width);
        m_DrawLinePaint.setStrokeCap(Paint.Cap.ROUND);
        m_DrawLinePaint.setAntiAlias(true);
        m_DrawLineCanvas.drawLine(x1, y1, x2, y2, m_DrawLinePaint);
    }

    public static void java_DrawPath(Bitmap bmpDest, int[] points, float width, int color, int style) {
        if (m_DrawPathCanvas == null)
            m_DrawPathCanvas = new Canvas(bmpDest);
        else
            m_DrawPathCanvas.setBitmap(bmpDest);


        if (style == 0)
            m_DrawPathPaint.setStyle(Paint.Style.FILL);
        else if (style == 2)
            m_DrawPathPaint.setStyle(Paint.Style.STROKE);
        else if (style == 3)
            m_DrawPathPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        else {
            assert (false);
        }

        m_DrawPathPaint.setColor(color);
        m_DrawPathPaint.setStrokeWidth(width);
        m_DrawPathPaint.setStrokeCap(Paint.Cap.ROUND);
        m_DrawPathPaint.setAntiAlias(true);
        m_Path.rewind();
        for (int i = 0; i < points.length - 1; i += 2) {
            if (i == 0)
                m_Path.moveTo(points[i], points[i + 1]);
            else
                m_Path.lineTo(points[i], points[i + 1]);
        }
        m_DrawPathCanvas.drawPath(m_Path, m_DrawPathPaint);
    }

    public static void java_DrawText(Bitmap bmpDest, Paint paint, int x, int y, String text, int nTextColor) {
        Canvas canvas = new Canvas(bmpDest);
        paint.setColor(nTextColor);//Color.argb(255, Color.red(nTextColor),Color.green(nTextColor),Color.blue(nTextColor)));
        int height = java_getTextHeight(paint, text, false);
/*
		Paint p1 = new Paint(paint);
		p1.setColor(0xffffff00);		
		Rect bounds = new Rect(0,0,0,0);		
		bounds.offset(x, y+height);
		bounds.left =x;
		bounds.top = y+height;
		bounds.bottom = y;
		bounds.right = x + (int)paint.measureText(text);
		if(text=="!")
		{
			int g=0;
			g++;
		}
		canvas.drawRect(bounds, p1);
	*/
        canvas.drawText(text, x, y + height, paint);


    }

    public static Bitmap java_CreateMemoryBitmap(int width, int height, int bitcount) {
        Bitmap bmp = null;

        switch (bitcount) {
            case 16: // RGB 565
                bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                break;

            case 32: // ARGB 8888
                bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                break;

            case 8:
            case 24:
            default:
                return null;
        }

        return bmp;
    }

    public static ByteBuffer java_CreateMemoryBitmapBytes(int sizeBytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(sizeBytes);
        return byteBuffer;
    }

    public static int java_CopyMemoryToBitmap(Bitmap bmp, ByteBuffer buffer) {
        try {
            buffer.rewind();
            bmp.copyPixelsFromBuffer(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    public static void java_BitBlt(Bitmap bmpDest, int nXDest, int nYDest, int nWidth, int nHeight, Bitmap bmpSrc) {
        m_BitBltCanvas.setBitmap(bmpDest);


        Rect rectDest = new Rect(nXDest, nYDest, nXDest + nWidth, nYDest + nHeight);

        m_BitBltCanvas.drawBitmap(bmpSrc, rectDest, rectDest, null);


    }

    public static void java_CopyBitmapToMemory(Bitmap bmp, ByteBuffer bufSrc) {
        try {
            bufSrc.rewind();
            //ByteOrder g = bufSrc.order();
            //bufSrc.order(ByteOrder.LITTLE_ENDIAN);
            bmp.copyPixelsToBuffer(bufSrc);
			/*
			Byte g = bufSrc.get(0);
			if(g!=0)
			{
				int gg=0;
				gg++;
			}			
			/*
			ByteBuffer ss = ByteBuffer.allocateDirect(bufSrc.capacity());
			ss.flip();
			ss.order(ByteOrder.LITTLE_ENDIAN);
			ss.putShort(g);
			short q = ss.getShort(0);
			
			ss.flip();
			ss.order(ByteOrder.BIG_ENDIAN);
			ss.putShort(g);
			q = ss.getShort(0);
			*/
            //short tt = ss.getShort(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void java_debugShowBmp(final Bitmap bmpDest, final int nXDest, final int nYDest, final int nWidth, final int nHeight) {
        try {
            final Bitmap bbb = bmpDest.copy(bmpDest.getConfig(), false);

            UI.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RelativeLayout view = UI.getMainView();
                    ImageView img = (ImageView) view.findViewWithTag(9876896);
                    if (img == null) {
                        img = new ImageView(view.getContext());
                        img.setTag(9876896);
                        view.addView(img);
                    }
                    img.setImageBitmap(bbb);
                    img.setAdjustViewBounds(true); // set the ImageView bounds to match the Drawable's dimensions
                    //img.setLayoutParams(new Gallery.LayoutParams(LayoutParams.WRAP_CONTENT,
                    // LayoutParams.WRAP_CONTENT));

					  
					/*
					LayoutParams lp = (LayoutParams) img.getLayoutParams();
					//lp.setMargins(0,0 ,150, 150);
					//lp.width = 50;
					//lp.height = 50;					 					
					img.setImageBitmap(bbb);
					img.requestLayout();
					*/
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int java_DrawText(CoreSurfaceView view, int x, int y, String text) {
        Canvas canvas = null;
        Rect bounds = new Rect();

        if (view == null)
            return -1;

        String s = new String(text);
        s += ".";
        view.paint.getTextBounds(s, 0, s.length(), bounds);

        canvas = view.getHolder().lockCanvas();
        if (canvas != null) {
            canvas.clipRect(bounds.left + x, bounds.top + y, bounds.right + x, bounds.bottom + y, Op.REPLACE);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawText(text, x, y, view.paint);
            view.getHolder().unlockCanvasAndPost(canvas);
        }

        canvas = view.getHolder().lockCanvas();
        if (canvas != null) {
            canvas.clipRect(bounds.left + x, bounds.top + y, bounds.right + x, bounds.bottom + y, Op.REPLACE);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawText(text, x, y, view.paint);
            view.getHolder().unlockCanvasAndPost(canvas);
            return 0;
        }

        return -1;
    }

    public static long getTotalMemory() {
        String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();//meminfo
            arrayOfString = str2.split("\\s+");

            //for (String num : arrayOfString)
            //{
            //Log.i(str2, num + "\t");
            //}

            //total Memory
            initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;

            localBufferedReader.close();


        } catch (IOException e) {
            return -1;
        }

        return initial_memory;
    }

    public static String java_getOSVersion() {
        String strOSVersion = Build.VERSION.RELEASE;
        return strOSVersion;
    }

}
 