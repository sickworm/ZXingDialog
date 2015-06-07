/*
 * Copyright (C) 2015 excelsecu authors
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

package com.excelsecu.zxing.capturedialog;

import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.provider.Browser;

import com.excelsecu.zxing.util.LogUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivityHandler;
import com.google.zxing.client.android.Res;
import com.google.zxing.client.android.ViewfinderResultPointCallback;
import com.google.zxing.client.android.camera.CameraManager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.Collection;
import java.util.Map;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureDialogHandler extends Handler {

  private static final String TAG = CaptureActivityHandler.class.getSimpleName();

  private final CaptureDialog dialog;
  private final DecodeThread decodeThread;
  private State state;
  private final CameraManager cameraManager;

  private enum State {
    PREVIEW,
    SUCCESS,
    DONE
  }

  CaptureDialogHandler(CaptureDialog dialog,
                         Collection<BarcodeFormat> decodeFormats,
                         Map<DecodeHintType,?> baseHints,
                         String characterSet,
                         CameraManager cameraManager) {
    this.dialog = dialog;
    decodeThread = new DecodeThread(dialog, decodeFormats, baseHints, characterSet,
        new ViewfinderResultPointCallback(dialog.getViewfinderView()));
    decodeThread.start();
    state = State.SUCCESS;

    // Start ourselves capturing previews and decoding.
    this.cameraManager = cameraManager;
    cameraManager.startPreview();
    restartPreviewAndDecode();
  }

  @Override
  public void handleMessage(Message message) {
    switch (message.what) {
      case Res.id.restart_preview:
        restartPreviewAndDecode();
        break;
      case Res.id.decode_succeeded:
        state = State.SUCCESS;
        Bundle bundle = message.getData();
        Bitmap barcode = null;
        if (bundle != null) {
          byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
          if (compressedBitmap != null) {
            barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
            // Mutable copy:
            barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
          }       
        }
        dialog.handleDecode((Result) message.obj, barcode);
        break;
      case Res.id.decode_failed:
        // We're decoding as fast as possible, so when one decode fails, start another.
        state = State.PREVIEW;
        cameraManager.requestPreviewFrame(decodeThread.getHandler(), Res.id.decode);
        break;
      case Res.id.return_scan_result:
          //dialog.setResult(Activity.RESULT_OK, (Intent) message.obj);
          dialog.dismiss();
        break;
      case Res.id.launch_product_query:
        String url = (String) message.obj;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.setData(Uri.parse(url));

        ResolveInfo resolveInfo =
                dialog.getContext().getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String browserPackageName = null;
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
          browserPackageName = resolveInfo.activityInfo.packageName;
          LogUtil.d(TAG, "Using browser in package " + browserPackageName);
        }

        // Needed for default Android browser / Chrome only apparently
        if ("com.android.browser".equals(browserPackageName) || "com.android.chrome".equals(browserPackageName)) {
          intent.setPackage(browserPackageName);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent.putExtra(Browser.EXTRA_APPLICATION_ID, browserPackageName);
        }

        try {
            dialog.getContext().startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
          LogUtil.w(TAG, "Can't find anything to handle VIEW of URI " + url);
        }
        break;
    }
  }

  public void quitSynchronously() {
    state = State.DONE;
    cameraManager.stopPreview();
    Message quit = Message.obtain(decodeThread.getHandler(), Res.id.quit);
    quit.sendToTarget();
    try {
      // Wait at most half a second; should be enough time, and onPause() will timeout quickly
      decodeThread.join(500L);
    } catch (InterruptedException e) {
      // continue
    }

    // Be absolutely sure we don't send any queued up messages
    removeMessages(Res.id.decode_succeeded);
    removeMessages(Res.id.decode_failed);
  }

  private void restartPreviewAndDecode() {
    if (state == State.SUCCESS) {
      state = State.PREVIEW;
      cameraManager.requestPreviewFrame(decodeThread.getHandler(), Res.id.decode);
      dialog.drawViewfinder();
    }
  }

}