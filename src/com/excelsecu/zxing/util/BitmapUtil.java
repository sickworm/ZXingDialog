package com.excelsecu.zxing.util;

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

public class BitmapUtil {
    protected final static String TAG = BitmapUtil.class.getSimpleName();
    private final static String FOLDER = "/sdcard/zxingtest";
    private static int count = 1;
    
    public static void clearDir() {
        File dir = new File(FOLDER);
        if (!dir.isDirectory()) {
            dir.mkdir();
        } else {
            File[] files = dir.listFiles();
            for (File file : files) {
                file.delete();
            }
        }
    }

    public static void decodeYUV420ToRGB565(int[] rgb, byte[] yuv420sp, int width,
            int height) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    public static Bitmap createBitmapfromYUV420(byte[] yuv420sp, int width, int height) {
        int[] rgb = new int[width * height];
        decodeYUV420ToRGB565(rgb, yuv420sp, width, height);
        Bitmap bitmap = Bitmap.createBitmap(rgb, width, height,
                Config.RGB_565);
        return bitmap;
    }

    public static void saveBitmap(Bitmap bitmap) {
        File dir = new File(FOLDER);
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        File file = new File(dir.getPath() + "/" + count++ + ".bmp");
        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }
            LogUtil.i(TAG, "save " + file.getName() + " succeed");
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.i(TAG, "save " + file.getName() + " failed");
        }
    }

    public static void saveYUV420(byte[] yuv420sp, int width, int height) {
        saveBitmap(createBitmapfromYUV420(yuv420sp, width, height));
    }

    public static void saveYUV420(byte[] yuv420sp, int width, int height,
            int x, int y, int cropWidth, int cropHeight) {
        Bitmap originBitmap = createBitmapfromYUV420(yuv420sp, width, height);
        Bitmap cropBitmap = Bitmap.createBitmap(originBitmap, x, y, cropWidth, cropHeight);
        saveBitmap(cropBitmap);
    }
}
