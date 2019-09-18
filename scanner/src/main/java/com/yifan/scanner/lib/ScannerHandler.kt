package com.yifan.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Handler
import com.google.zxing.Result
import com.yifan.scanner.lib.camera.CameraManager

interface ScannerHandler {

    fun getCameraManager(): CameraManager

    fun getHandler(): Handler

    fun getContext(): Context

    fun getPackageManager(): PackageManager

    fun setResult(resultCode: Int, intent: Intent)

    fun finish()

    fun handleDecode(rawResult: Result, barcode: Bitmap?, scaleFactor: Float)

    fun startActivity(intent: Intent)

    fun getViewfinderView(): ViewfinderView

    fun drawViewfinder()
}