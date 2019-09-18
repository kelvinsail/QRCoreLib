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

package com.yifan.scanner.lib.camera

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.provider.Browser
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.yifan.scanner.*

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
class CaptureActivityHandler(
    private val scannerHandler: ScannerHandler,
    decodeFormats: Collection<BarcodeFormat>?,
    baseHints: Map<DecodeHintType, Any>?,
    characterSet: String,
    private val cameraManager: CameraManager
) : Handler() {
    private val decodeThread: DecodeThread
    private var state: State? = null

    private enum class State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    init {
        decodeThread = DecodeThread(
            scannerHandler, decodeFormats, baseHints, characterSet,
            ViewfinderResultPointCallback(scannerHandler.getViewfinderView())
        )
        decodeThread.start()
        state = State.SUCCESS
        cameraManager.startPreview()
        restartPreviewAndDecode()
    }// Start ourselves capturing previews and decoding.

    override fun handleMessage(message: Message) {
        when (message.what) {
            R.id.restart_preview -> restartPreviewAndDecode()
            R.id.decode_succeeded -> {
                state = State.SUCCESS
                val bundle = message.data
                var barcode: Bitmap? = null
                var scaleFactor = 1.0f
                if (bundle != null) {
                    val compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP)
                    if (compressedBitmap != null) {
                        barcode = BitmapFactory.decodeByteArray(
                            compressedBitmap,
                            0,
                            compressedBitmap.size,
                            null
                        )
                        // Mutable copy:
                        barcode = barcode!!.copy(Bitmap.Config.ARGB_8888, true)
                    }
                    scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR)
                }
                scannerHandler.handleDecode(message.obj as Result, barcode, scaleFactor)
            }
            R.id.decode_failed -> {
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW
                cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode)
            }
            R.id.return_scan_result -> {
                scannerHandler.setResult(Activity.RESULT_OK, message.obj as Intent)
                scannerHandler.finish()
            }
            R.id.launch_product_query -> {
                val url = message.obj as String

                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intents.FLAG_NEW_DOC)
                intent.data = Uri.parse(url)

                val resolveInfo = scannerHandler.getPackageManager()
                    .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                var browserPackageName: String? = null
                if (resolveInfo != null && resolveInfo!!.activityInfo != null) {
                    browserPackageName = resolveInfo!!.activityInfo.packageName
                    Log.d(TAG, "Using browser in package " + browserPackageName!!)
                }

                // Needed for default Android browser / Chrome only apparently
                if (browserPackageName != null) {
                    when (browserPackageName) {
                        "com.android.browser", "com.android.chrome" -> {
                            intent.setPackage(browserPackageName)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.putExtra(Browser.EXTRA_APPLICATION_ID, browserPackageName)
                        }
                    }
                }

                try {
                    scannerHandler.startActivity(intent)
                } catch (ignored: ActivityNotFoundException) {
                    Log.w(TAG, "Can't find anything to handle VIEW of URI")
                }

            }
        }
    }

    fun quitSynchronously() {
        state = State.DONE
        cameraManager.stopPreview()
        val quit = Message.obtain(decodeThread.getHandler(), R.id.quit)
        quit.sendToTarget()
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L)
        } catch (e: InterruptedException) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded)
        removeMessages(R.id.decode_failed)
    }

    private fun restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode)
            scannerHandler.drawViewfinder()
        }
    }

    companion object {

        private val TAG = CaptureActivityHandler::class.java.simpleName
    }

}
