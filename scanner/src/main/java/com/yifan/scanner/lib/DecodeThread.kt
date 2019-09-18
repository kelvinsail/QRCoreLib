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

package com.yifan.scanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPointCallback

import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import com.yifan.scanner.lib.camera.Constans
import java.util.EnumMap
import java.util.EnumSet
import java.util.concurrent.CountDownLatch

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
internal class DecodeThread(
    private val scannerHandler: ScannerHandler,
    decodeFormats: Collection<BarcodeFormat>?,
    baseHints: Map<DecodeHintType, Any>?,
    characterSet: String?,
    resultPointCallback: ResultPointCallback
) : Thread() {
    private val hints: MutableMap<DecodeHintType, Any>
    private var handler: Handler? = null
    private val handlerInitLatch: CountDownLatch

    init {
        var decodeFormats = decodeFormats
        handlerInitLatch = CountDownLatch(1)

        hints = EnumMap(DecodeHintType::class.java)
        if (baseHints != null) {
            hints.putAll(baseHints)
        }

        // The prefs can't change while the thread is running, so pick them up once here.
        if (decodeFormats == null || decodeFormats.isEmpty()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(scannerHandler.getContext())
            decodeFormats = EnumSet.noneOf(BarcodeFormat::class.java)
            if (prefs.getBoolean(Constans.KEY_DECODE_1D_PRODUCT, true)) {
                decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS)
            }
            if (prefs.getBoolean(Constans.KEY_DECODE_1D_INDUSTRIAL, true)) {
                decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS)
            }
            if (prefs.getBoolean(Constans.KEY_DECODE_QR, true)) {
                decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS)
            }
            if (prefs.getBoolean(Constans.KEY_DECODE_DATA_MATRIX, true)) {
                decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS)
            }
            if (prefs.getBoolean(Constans.KEY_DECODE_AZTEC, false)) {
                decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS)
            }
            if (prefs.getBoolean(Constans.KEY_DECODE_PDF417, false)) {
                decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS)
            }
        }
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats

        if (characterSet != null) {
            hints[DecodeHintType.CHARACTER_SET] = characterSet
        }
        hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = resultPointCallback
    }

    fun getHandler(): Handler? {
        try {
            handlerInitLatch.await()
        } catch (ie: InterruptedException) {
            // continue?
        }

        return handler
    }

    override fun run() {
        Looper.prepare()
        handler = DecodeHandler(scannerHandler, hints)
        handlerInitLatch.countDown()
        Looper.loop()
    }

    companion object {

        public val BARCODE_BITMAP = "barcode_bitmap"
        public val BARCODE_SCALED_FACTOR = "barcode_scaled_factor"
    }

}
