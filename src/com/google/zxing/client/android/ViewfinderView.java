/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
  private static final long ANIMATION_DELAY = 80L;
  private static final int CURRENT_POINT_OPACITY = 0xA0;
  private static final int MAX_RESULT_POINTS = 20;
  private static final int POINT_SIZE = 6;

  private CameraManager cameraManager;
  private final Paint paint;
  private Bitmap resultBitmap;
  private final int maskColor;
  private final int resultColor;
  private final int laserColor;
  private final int resultPointColor;
  private int scannerAlpha;
  private List<ResultPoint> possibleResultPoints;
  private List<ResultPoint> lastPossibleResultPoints;

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    maskColor = 0x60000000;
    resultColor = 0xB0000000;
    laserColor = 0xFFCC0000;
    resultPointColor = 0xC0FFBD21;
    scannerAlpha = 0;
    possibleResultPoints = new ArrayList<>(5);
    lastPossibleResultPoints = null;
  }

  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }

  @SuppressLint("DrawAllocation")
  @Override
  public void onDraw(Canvas canvas) {
    if (cameraManager == null) {
      return; // not ready yet, early draw before done configuring
    }
    Rect frame = cameraManager.getFramingRect();
    Rect previewFrame = cameraManager.getFramingRectInPreview();
    if (frame == null || previewFrame == null) {
      return;
    }
    int width = getWidth();
    int height = getHeight();
    //@ch decrease status bar height
    //why we should / 2? Because the preview was scaled instead of offset
    //it's not a perfect solution, we should use canvas resolution to calculate cameraManager.getFramingRect()
    //but we haven't fix this, maybe later
    frame.top -= getStatusBarHeight() / 2;
    frame.bottom -= getStatusBarHeight() / 2;

    // Draw the exterior (i.e. outside the framing rect) darkened
    paint.setColor(resultBitmap != null ? resultColor : maskColor);
    canvas.drawRect(0, 0, width, frame.top, paint);
    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
    canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    //@ch draw the rim
    int rimColor = 0xAA00AA00;
    int rimWidth = frame.width() / 20;
    int rimLength = frame.width() / 6;
    paint.setColor(rimColor);
    canvas.drawRect(frame.left - rimWidth, frame.top - rimWidth, frame.left + rimLength, frame.top, paint);
    canvas.drawRect(frame.left - rimWidth, frame.top, frame.left, frame.top + rimLength, paint);
    canvas.drawRect(frame.right - rimLength, frame.top - rimWidth, frame.right + rimWidth, frame.top, paint);
    canvas.drawRect(frame.right, frame.top, frame.right + rimWidth, frame.top + rimLength, paint);
    canvas.drawRect(frame.left - rimWidth, frame.bottom, frame.left + rimLength, frame.bottom + rimWidth, paint);
    canvas.drawRect(frame.left - rimWidth, frame.bottom - rimLength, frame.left, frame.bottom, paint);
    canvas.drawRect(frame.right - rimLength, frame.bottom, frame.right + rimWidth, frame.bottom + rimWidth, paint);
    canvas.drawRect(frame.right,  frame.bottom - rimLength, frame.right + rimWidth, frame.bottom + rimWidth, paint);

    if (resultBitmap != null) {
      // Draw the opaque result bitmap over the scanning rectangle
      paint.setAlpha(CURRENT_POINT_OPACITY);
      canvas.drawBitmap(resultBitmap, null, frame, paint);
    } else {

      // Draw a red "laser scanner" line through the middle to show decoding is active
      paint.setColor(laserColor);
      paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
      int middle = frame.height() / 2 + frame.top;
      //@ch control the width of laser
      int laserHalfWidth = frame.width() / 80;
      canvas.drawRect(frame.left + 2, middle - laserHalfWidth, frame.right - 1, middle + laserHalfWidth, paint);

      float scaleX = frame.width() / (float) previewFrame.width();
      float scaleY = frame.height() / (float) previewFrame.height();

      List<ResultPoint> currentPossible = possibleResultPoints;
      List<ResultPoint> currentLast = lastPossibleResultPoints;
      int frameLeft = frame.left;
      int frameTop = frame.top;
      if (currentPossible.isEmpty()) {
        lastPossibleResultPoints = null;
      } else {
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = currentPossible;
        paint.setAlpha(CURRENT_POINT_OPACITY);
        paint.setColor(resultPointColor);
        synchronized (currentPossible) {
          for (ResultPoint point : currentPossible) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              POINT_SIZE, paint);
          }
        }
      }
      if (currentLast != null) {
        paint.setAlpha(CURRENT_POINT_OPACITY / 2);
        paint.setColor(resultPointColor);
        synchronized (currentLast) {
          float radius = POINT_SIZE / 2.0f;
          for (ResultPoint point : currentLast) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              radius, paint);
          }
        }
      }

      //@ch draw the text
      int margin = canvas.getWidth() / 20;
      int rectWidth = canvas.getWidth() / 7;
      int textSize = canvas.getWidth() / 20;
      Rect targetRect = new Rect(margin, margin, canvas.getWidth() - margin, rectWidth + margin);
      Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setTextSize(textSize);
      String testString = "请将二维码放入扫描框中";
      paint.setColor(Color.CYAN);
      paint.setAlpha(0xBB);
      canvas.drawRect(targetRect, paint);
      paint.setColor(Color.WHITE);
      FontMetricsInt fontMetrics = paint.getFontMetricsInt();
          // 转载请注明出处：http://blog.csdn.net/hursing
      int baseline = targetRect.top + (targetRect.bottom - targetRect.top - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
      // 下面这行是实现水平居中，drawText对应改为传入targetRect.centerX()
      paint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText(testString, targetRect.centerX(), baseline, paint);

      // Request another update at the animation interval, but only repaint the laser line,
      // not the entire viewfinder mask.
      postInvalidateDelayed(ANIMATION_DELAY,
                            frame.left - POINT_SIZE,
                            frame.top - POINT_SIZE,
                            frame.right + POINT_SIZE,
                            frame.bottom + POINT_SIZE);
    }
  }

  public void drawViewfinder() {
    Bitmap resultBitmap = this.resultBitmap;
    this.resultBitmap = null;
    if (resultBitmap != null) {
      resultBitmap.recycle();
    }
    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    resultBitmap = barcode;
    invalidate();
  }

  public void addPossibleResultPoint(ResultPoint point) {
    List<ResultPoint> points = possibleResultPoints;
    synchronized (points) {
      points.add(point);
      int size = points.size();
      if (size > MAX_RESULT_POINTS) {
        // trim it
        points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
      }
    }
  }


  /**
   * calculate status bar height
   * @ch
   * @return
   */
  public int getStatusBarHeight() {
      Class<?> c = null;
      Object obj = null;
      Field field = null;
      int x = 0, sbar = 0;
      try {
          c = Class.forName("com.android.internal.R$dimen");
          obj = c.newInstance();
          field = c.getField("status_bar_height");
          x = Integer.parseInt(field.get(obj).toString());
          sbar = getResources().getDimensionPixelSize(x);
      } catch(Exception e1) {
          e1.printStackTrace();
      }
      return sbar;
  }
}
