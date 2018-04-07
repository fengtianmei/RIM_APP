package com.skyline.terraexplorer.models;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileUtils {
    public static void CopyFile(String source, String destination) {
        try {
            FileInputStream in = new FileInputStream(source);
            FileOutputStream out = new FileOutputStream(destination);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file (You have now copied the file)
            out.flush();
            out.close();
            out = null;
        } catch (Exception ex) {
            Log.e("tag", ex.getMessage());
        }
    }

    public static void CopyFiles(String sourceDirectory, String fileNameWithoutExtention, String destinationDirectory) {
        for (File file : new File(sourceDirectory).listFiles()) {
            if (file.isFile()) {
                if (file.getName().startsWith(fileNameWithoutExtention))
                    CopyFile(sourceDirectory + File.separator + file.getName(), destinationDirectory + File.separator + file.getName());
            }
        }
    }
}
