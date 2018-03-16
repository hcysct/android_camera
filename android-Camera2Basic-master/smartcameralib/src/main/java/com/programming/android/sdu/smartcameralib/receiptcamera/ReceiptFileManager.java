package com.programming.android.sdu.smartcameralib.receiptcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.text.format.DateFormat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by grzegorzbaczek on 16/03/2018.
 */

public class ReceiptFileManager {

    private List<String> receiptFiles;


    public ReceiptFileManager(){
        receiptFiles = new ArrayList<>();
    }

    public void AddFile(String file){
        receiptFiles.add(file);
    }

    public File createPdfFromFiles( File externalFileDir, int width, int height, Context context) {


        String timeStamp = new SimpleDateFormat("yy_MM_dd_HH_mm_ss_SSS").format(Calendar.getInstance().getTime());
        String filename = String.format("receipt_%s.pdf", timeStamp);
        File file = new File(externalFileDir, filename);

        Document document = new Document();
        document.setMargins(0 , 0 , 0 , 0);
        try {

            PdfWriter.getInstance(document,new FileOutputStream(file));
            document.open();
            for (String filePath : receiptFiles) {
                Image image = Image.getInstance(filePath);
                image.setAlignment(Image.MIDDLE);
                image.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight() );
                document.add(image);
            }
            document.close();
            return file;

        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }  catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
