package com.skyline.terraexplorer.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import com.skyline.teapi.ISGWorld;
import com.skyline.terraexplorer.TEApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

public class TEImageHelper {
    private static String TEAppData = null;
    private static int ImageSize = 0;

    public static String prepareImageForTE(int imageId) {
        return prepareImageForTE(imageId, Integer.toString(imageId));
    }

    public static String prepareImageForTE(int imageId, String imageName) {
        if (TEAppData == null) {
            TEAppData = UI.runOnRenderThread(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return ISGWorld.getInstance().getApplication().getDataPath();
                }
            });
            // bug fix #18757
            // for some reason 65 works for all our devices, while using 48/65 does not :(
            ImageSize = 65;
        }
        Options opt = new Options();
        opt.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(TEApp.getAppContext().getResources(), imageId, opt);
        bmp = Bitmap.createScaledBitmap(bmp, ImageSize, ImageSize, true);
        String filepath = TEAppData + File.separator + imageName + ".png";
        FileOutputStream out;
        try {
            out = new FileOutputStream(filepath);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return filepath;
    }
}
