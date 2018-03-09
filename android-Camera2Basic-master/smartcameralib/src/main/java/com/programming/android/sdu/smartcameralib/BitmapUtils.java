package com.programming.android.sdu.smartcameralib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by grzegorzbaczek on 09/03/2018.
 */

public class BitmapUtils {

    public static void saveToFile(String outFilePath, Bitmap bmp, int quality) {
        saveToFile(new File(outFilePath), bmp, quality);
    }
    public static void saveToFile(File file, Bitmap bmp, int quality) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.flush();
            out.close();
        } catch (Exception e) {
        }
    }

    public static void resizeToMaxSize(String outputFilePath, String inputFilePath) {
        long fileSize = getFileSizeInKb(new File(inputFilePath));
        int quality = 95;
        FileOutputStream out = null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(inputFilePath, options);

        while(fileSize > Constants.MAX_BITMAP_SIZE_KB){
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            byte[] byteArray = stream.toByteArray();
            fileSize = byteArray.length / 1024;
            quality -= 2;
            Log.i("img_preview", String.format("quality %d", quality));
        }
        saveToFile(outputFilePath, bitmap, quality);

        Log.i("img_preview", String.format("compressed file size %d Kb", getFileSizeInKb(new File(outputFilePath))));



    }

    public static long getFileSizeInKb(File f) {
        long size = f.length();
        return size / 1024;
    }
}
