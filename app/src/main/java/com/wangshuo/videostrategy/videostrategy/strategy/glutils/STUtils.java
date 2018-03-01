package com.wangshuo.videostrategy.videostrategy.strategy.glutils;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class STUtils {
    private String TAG = "STUtils";
    private Context mContext;

    // track config
    private static final int ST_MOBILE_TRACKING_MULTI_THREAD = 0x00000000;
    private static final int ST_MOBILE_TRACKING_ENABLE_DEBOUNCE  = 0x00000010;
    private static final int ST_MOBILE_TRACKING_ENABLE_FACE_ACTION   = 0x00000020;
    private static final int ST_MOBILE_TRACKING_DEFAULT_CONFIG = ST_MOBILE_TRACKING_MULTI_THREAD;

    public STUtils(Context context) {
        this.mContext = context;
    }

    public static int[] getBGRAImageByte(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if(image.getConfig().equals(Bitmap.Config.ARGB_8888)) {
            int[] imgData = new int[width * height];
            image.getPixels(imgData, 0, width, 0, 0, width, height);
            return imgData;
        } else {
            // TODO
        }

        return null;
    }

    public static byte[] getBGRFromBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int componentsPerPixel = 3;
        int totalPixels = width * height;
        int totalBytes = totalPixels * componentsPerPixel;

        byte[] rgbValues = new byte[totalBytes];
         int[] argbPixels = new int[totalPixels];
        bitmap.getPixels(argbPixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < totalPixels; i++) {
             int argbPixel = argbPixels[i];
            int red = Color.red(argbPixel);
            int green = Color.green(argbPixel);
            int blue = Color.blue(argbPixel);
            rgbValues[i * componentsPerPixel + 0] = (byte) blue;
            rgbValues[i * componentsPerPixel + 1] = (byte) green;
            rgbValues[i * componentsPerPixel + 2] = (byte) red;
        }

        return rgbValues;
    }

    public static Bitmap getBitmapFromBGR(byte[] data, int width, int height){
        int[] argb = new int[width*height];
        for (int i=0; i<height; i++) {
            for (int j=0; j<width; j++) {
                argb[i * width + j] = data[(i*width + j) * 3 + 0] & 0xFF  //b
                        | (data[(i*width + j) * 3 + 1] & 0xFF) << 8       //g
                        | (data[(i*width + j) * 3 + 2] & 0xFF) << 16      //r
                        | (0xFF) << 24;                                   //a
            }
        }

        return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
    }

    public static Bitmap getBitmapFromRGBA(byte[] data, int width, int height){
        int[] argb = new int[width*height];
        for (int i=0; i<height; i++) {
            for (int j=0; j<width; j++) {
                argb[i * width + j] = 255;
                argb[i * width + j] = (argb[i * width + j] << 8) + data[(i*width + j) * 4 + 0]; //+r
                argb[i * width + j] = (argb[i * width + j] << 8) + data[(i*width + j) * 4 + 1]; //+g
                argb[i * width + j] = (argb[i * width + j] << 8) + data[(i*width + j) * 4 + 2]; //+b
            }
        }

        return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
    }

    public static byte[] Bitmap2Bytes(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public static Bitmap getBitmapFromFile(Uri uri){
        if(uri == null){
            return null;
        }

        Bitmap bmp = null;
        BitmapFactory.Options opts=new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        bmp = BitmapFactory.decodeFile(uri.getPath(), opts);
        opts.inSampleSize = computeSampleSize(opts);
        opts.inJustDecodeBounds = false;
        bmp = BitmapFactory.decodeFile(uri.getPath(),opts);

        return bmp;
    }

    public static Bitmap getBitmapAfterRotate(Uri uri, Context context) {
        Bitmap rotatebitmap = null;
        Bitmap srcbitmap = null;
        String[] filePathColumn = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION};
        Cursor cursor = null;
        String picturePath = null;
        String orientation = null;

        try {
            cursor = context.getContentResolver().query(uri,filePathColumn, null, null, null);

            if(cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                picturePath = cursor.getString(columnIndex);
                orientation = cursor.getString(cursor.getColumnIndex(filePathColumn[1]));
            }
        } catch (SQLiteException e) {
            // Do nothing
        } catch (IllegalArgumentException e) {
            // Do nothing
        } catch (IllegalStateException e) {
            // Do nothing
        } finally {
            if(cursor != null)
                cursor.close();
        }
        if(picturePath != null) {
            int angle = 0;
            if (orientation != null && !"".equals(orientation)) {
                angle = Integer.parseInt(orientation);
            }

            BitmapFactory.Options opts=new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            srcbitmap = BitmapFactory.decodeFile(picturePath, opts);

            opts.inSampleSize = computeSampleSize(opts);
            opts.inJustDecodeBounds = false;
            srcbitmap = BitmapFactory.decodeFile(picturePath,opts);
            if (angle != 0) {
                // 下面的方法主要作用是把图片转一个角度，也可以放大缩小等
                Matrix m = new Matrix();
                int width = srcbitmap.getWidth();
                int height = srcbitmap.getHeight();
                m.setRotate(angle); // 旋转angle度
                try {
                    rotatebitmap = Bitmap.createBitmap(srcbitmap, 0, 0, width, height,m, true);// 新生成图片
                } catch(Exception e) {

                }

                if(rotatebitmap == null) {
                    rotatebitmap = srcbitmap;
                }

                if(srcbitmap != rotatebitmap) {
                    srcbitmap.recycle();
                }
            }
            else {
                rotatebitmap = srcbitmap;
            }
        }

        return rotatebitmap;
    }

    public static Bitmap getBitmapFromFileAfterRotate(Uri uri) {
        if(uri == null){
            return null;
        }

        Bitmap rotatebitmap = null;
        Bitmap srcbitmap = null;

        String picturePath = uri.getPath();
        if(picturePath != null) {
            int angle = 0;

            angle = getBitmapDegree(picturePath);

            BitmapFactory.Options opts=new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            srcbitmap = BitmapFactory.decodeFile(picturePath, opts);

            opts.inSampleSize = computeSampleSize(opts);
            opts.inJustDecodeBounds = false;
            srcbitmap = BitmapFactory.decodeFile(picturePath,opts);
            if (angle != 0) {
                // 下面的方法主要作用是把图片转一个角度，也可以放大缩小等
                Matrix m = new Matrix();
                int width = srcbitmap.getWidth();
                int height = srcbitmap.getHeight();
                m.setRotate(angle); // 旋转angle度
                try {
                    rotatebitmap = Bitmap.createBitmap(srcbitmap, 0, 0, width, height,m, true);// 新生成图片
                } catch(Exception e) {

                }

                if(rotatebitmap == null) {
                    rotatebitmap = srcbitmap;
                }

                if(srcbitmap != rotatebitmap) {
                    srcbitmap.recycle();
                }
            }
            else {
                rotatebitmap = srcbitmap;
            }
        }

        return rotatebitmap;
    }

    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public static Bitmap getBitmapFromDrawable(Context context, int resId){
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opt.inSampleSize = 2;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        //获取资源图片
        InputStream is = context.getResources().openRawResource(resId);
        return BitmapFactory.decodeStream(is,null,opt);
    }

    public static int computeSampleSize(BitmapFactory.Options opts) {
        int sampleSize = 1;
        int width = opts.outWidth;
        int height = opts.outHeight;
        if(width > 2048 || height > 2048) {
            sampleSize = 4;
        }else if(width > 1024 || height > 1024){
            sampleSize = 2;
        }

        return sampleSize;
    }

    public static void copyFilesToLocalIfNeed(Context context) {
        String assertPathDir = "filter";
        String dirPath = context.getFilesDir().getAbsolutePath() + File.separator + "filter";

        File pictureDir = new File(dirPath);
        if (!pictureDir.exists() || !pictureDir.isDirectory()) {
            pictureDir.mkdirs();
        }
        try {
            String[] fileNames = context.getAssets().list(assertPathDir);
            if (fileNames.length == 0)
                return;
            for (int i = 0; i < fileNames.length; i++) {
                File file = new File(dirPath + File.separator + fileNames[i]);
                if (file.exists() && file.isFile()) {
                    if (compareFile(context, dirPath + File.separator + fileNames[i], assertPathDir + File.separator + fileNames[i])) {
                        //printLog("-->copyAssertDirToLocalIfNeed " + file.getName() + " exists");
                        continue;
                    }
                }
                InputStream is = context.getAssets().open(assertPathDir + File.separator + fileNames[i]);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String mypath = dirPath + File.separator + fileNames[i];
                FileOutputStream fop = new FileOutputStream(mypath);
                fop.write(buffer);
                fop.flush();
                fop.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean compareFile(Context context, String filePath, String assetPath) {
        boolean isSameFile = false;
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) {
            isSameFile = false;
        }
        if (getFileSize(file) == getAssertFileSize(context, assetPath)) {
            isSameFile = true;
        }
        return isSameFile;
    }

    /**
     * 获取指定文件大小
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static long getFileSize(File file) {
        long size = 0;
        if (!file.exists() || file.isDirectory()) {
            //printLog("getFileSize file is not exists or isDirectory !");
            return 0;
        }
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                size = fis.available();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return size;
    }

    /**
     * 获取Asset目录下某个文件的大小，非目录
     *
     * @param context
     * @param path
     * @return
     */
    public static long getAssertFileSize(Context context, String path) {
        if (context == null || path == null || "".equals(path)) {
            //printLog("getAssertFileSize context is null or path is null !");
            return 0;
        }
        //printLog("getAssertFileSize path:"+path);
        AssetManager assetManager = context.getAssets();
        int size = 0;
        try {
            InputStream inStream = assetManager.open(path);
            size = inStream.available();
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    public void copyModelIfNeed(String modelName) {
        String path = getModelPath(modelName);
        if (path != null) {
            File modelFile = new File(path);
            if (!modelFile.exists()) {
                //如果模型文件不存在或者当前模型文件的版本跟sdcard中的版本不一样
                try {
                    if (modelFile.exists())
                        modelFile.delete();
                    modelFile.createNewFile();
                    InputStream in = mContext.getApplicationContext().getAssets().open(modelName);
                    if(in == null)
                    {
                        Log.e(TAG, "the src module is not existed");
                    }
                    OutputStream out = new FileOutputStream(modelFile);
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    modelFile.delete();
                }
            }
        }
    }

    public String getModelPath(String modelName) {
        String path = null;
        File dataDir = mContext.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + modelName;
        }
        return path;
    }

    public int getCameraTrackConfig() {
        int config = ST_MOBILE_TRACKING_DEFAULT_CONFIG | ST_MOBILE_TRACKING_ENABLE_DEBOUNCE | ST_MOBILE_TRACKING_ENABLE_FACE_ACTION;
        return config;
    }

    public static Rect adjustToImageRect(Rect intputRect, int screenWidth, int screenHeight, int imageWidth, int imageHeight){
        Rect rect = new Rect();

        if((float)screenHeight/screenWidth >= (float)imageHeight/imageWidth){
            int gap = (screenHeight - screenWidth * imageHeight/imageWidth)/2;

            if(intputRect.top <= gap){
                rect.top = 0;
                rect.bottom = intputRect.height() * imageWidth/screenWidth;
            }else if(intputRect.bottom + gap >= screenHeight){
                rect.bottom = imageHeight - 1;
                rect.top = rect.bottom - intputRect.height() * imageWidth/screenWidth;
            }else{
                rect.top = (intputRect.top - gap) * imageWidth/screenWidth;
                rect.bottom = (intputRect.bottom - gap) * imageWidth/screenWidth;
            }

            if(intputRect.left < 0){
                rect.left = 0;
                rect.right = intputRect.width() * imageWidth/screenWidth;
            }else if(intputRect.right >= screenWidth){
                rect.right = imageWidth - 1;
                rect.left = rect.right - intputRect.width() * imageWidth/screenWidth;
            }else{
                rect.left = intputRect.left * imageWidth/screenWidth;
                rect.right = intputRect.right * imageWidth/screenWidth;
            }
        }else{
            int gap = (screenWidth - (screenHeight* imageWidth/imageHeight))/2;

            if(intputRect.top < 0){
                rect.top = 0;
                rect.bottom = intputRect.height() * imageHeight/screenHeight;
            }else if(intputRect.bottom >= screenHeight){
                rect.bottom = imageHeight - 1;
                rect.top = rect.bottom - intputRect.height() * imageHeight/screenHeight;
            }else{
                rect.top = intputRect.top * imageHeight/screenHeight;
                rect.bottom = intputRect.bottom * imageHeight/screenHeight;
            }

            if(intputRect.left <= gap){
                rect.left = 0;
                rect.right = intputRect.width() * imageHeight/screenHeight;
            }else if(intputRect.right + gap >= screenWidth){
                rect.right = imageWidth - 1;
                rect.left = rect.right - intputRect.width() * imageHeight/screenHeight;
            }else{
                rect.left = (intputRect.left - gap) * imageHeight/screenHeight;
                rect.right = (intputRect.right - gap) * imageHeight/screenHeight;
            }
        }

        return rect;
    }

    public static Rect adjustToScreenRect(Rect intputRect, int screenWidth, int screenHeight, int imageWidth, int imageHeight){
        Rect rect = intputRect;

        if((float)screenHeight/screenWidth >= (float)imageHeight/imageWidth){
            int gap = (int)(screenHeight - ((float)screenWidth/imageWidth * imageHeight))/2;

            rect.top = (int)(intputRect.top * (float)screenWidth/imageWidth) + gap;
            rect.bottom = (int)(intputRect.bottom * (float)screenWidth/imageWidth) + gap;

            rect.left = (int)(intputRect.left * (float)screenWidth/imageWidth);
            rect.right = (int)(intputRect.right * (float)screenWidth/imageWidth);
        }else{
            int gap = (int)(screenWidth - ((float)screenHeight/imageHeight * imageWidth))/2;

            rect.top = (int)(intputRect.top * (float)screenHeight/imageHeight);
            rect.bottom = (int)(intputRect.bottom * (float)screenHeight/imageHeight);

            rect.left = (int)(intputRect.left * (float)screenHeight/imageHeight) + gap;
            rect.right = (int)(intputRect.right * (float)screenHeight/imageHeight) + gap;
        }

        return rect;
    }

    public boolean deleteDirectory(String filePath) {
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        return true;
    }



}
