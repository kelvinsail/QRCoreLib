/*
 * Copyright (C) 2010 ZXing authors
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

package com.yifan.scanner

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

internal class DecodeHandler(
    private val scannerHandler: ScannerHandler,
    hints: Map<DecodeHintType, Any>
) : Handler() {
    private val multiFormatReader: MultiFormatReader
    private var running = true

    init {
        multiFormatReader = MultiFormatReader()
        multiFormatReader.setHints(hints)
    }

    override fun handleMessage(message: Message) {
        if (message == null || !running) {
            return
        }
        when (message.what) {
            R.id.decode -> decode(message.obj as ByteArray, message.arg1, message.arg2)
            R.id.quit -> {
                running = false
                Looper.myLooper()!!.quit()
            }
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private fun decode(data: ByteArray, width: Int, height: Int) {
        val start = System.nanoTime()
        var rawResult: Result? = null
        val source = scannerHandler.getCameraManager().buildLuminanceSource(data, width, height)
        if (source != null) {
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap)
            } catch (re: ReaderException) {
                // continue
            } finally {
                multiFormatReader.reset()
            }
        }

        val handler = scannerHandler.getHandler()
        if (rawResult != null) {
            // Don't log the barcode contents for security.
            val end = System.nanoTime()
            Log.d(TAG, "Found barcode in " + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms")
            if (handler != null) {
                val message = Message.obtain(handler, R.id.decode_succeeded, rawResult)
                val bundle = Bundle()
                bundleThumbnail(source!!, bundle)
                message.data = bundle
                message.sendToTarget()
            }
        } else {
            if (handler != null) {
                val message = Message.obtain(handler, R.id.decode_failed)
                message.sendToTarget()
            }
        }
    }

    companion object {

        private val TAG = DecodeHandler::class.java.simpleName

        private fun bundleThumbnail(source: PlanarYUVLuminanceSource, bundle: Bundle) {
            val pixels = source.renderThumbnail()
            val width = source.thumbnailWidth
            val height = source.thumbnailHeight
            val bitmap =
                Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
            bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray())
            bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, width.toFloat() / source.width)
        }
    }

}
