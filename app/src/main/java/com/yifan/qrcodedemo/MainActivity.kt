package com.yifan.qrcodedemo

import android.net.Uri
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission
import com.yifan.qrcodedemo.databinding.ActivityMainBinding
import com.yifan.scanner.AbstractScannerActivity
import com.yifan.scanner.DecodeFormatManager
import com.yifan.scanner.Intents
import com.yifan.scanner.Scanner
import com.yifan.scanner.lib.camera.CameraManager
import com.yifan.scanner.lib.camera.DecodeHintManager
import java.util.*


class MainActivity : AbstractScannerActivity() {

    lateinit var scannerHandler: Scanner


    companion object {
        val TAG = "MainActivity"
        val ZXING_URLS = arrayOf("http://zxing.appspot.com/scan", "zxing://scan/")
    }

    lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        AndPermission.with(this)
            .runtime()
            .permission(Permission.CAMERA)
            .onGranted {

            }
            .onDenied { permissions ->
                if (AndPermission.hasAlwaysDeniedPermission(this@MainActivity, permissions)) {

                }
            }.start()

        scannerHandler = Scanner(this, mBinding.vfvScan)

        if (intent != null) {
            val action = intent.action
            val dataString = intent.dataString
            if (Intents.Scan.ACTION.equals(action)) {
                // Scan the formats the intent requested, and return the result to the calling activity.
                // source = IntentSource.NATIVE_APP_INTENT;
                scannerHandler.decodeFormats = DecodeFormatManager.parseDecodeFormats(intent)
                scannerHandler.decodeHints = DecodeHintManager.parseDecodeHints(intent)
                if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
                    val width = intent.getIntExtra(Intents.Scan.WIDTH, 0)
                    val height = intent.getIntExtra(Intents.Scan.HEIGHT, 0)
                    if (width > 0 && height > 0) {
                        scannerHandler.getCameraManager().setManualFramingRect(width, height)
                    }
                }
                if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
                    val cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1)
                    if (cameraId >= 0) {
                        scannerHandler.getCameraManager().setManualCameraId(cameraId)
                    }
                }
            } else if (dataString != null &&
                dataString.contains("http://www.google") &&
                dataString.contains("/m/products/scan")
            ) {
                scannerHandler.decodeFormats = DecodeFormatManager.PRODUCT_FORMATS
            } else if (isZXingURL(dataString)) {

                val inputUri = Uri.parse(dataString)
                //scanFromWebPageManager = new ScanFromWebPageManager(inputUri);
                scannerHandler.decodeFormats = DecodeFormatManager.parseDecodeFormats(inputUri)
                // Allow a sub-set of the hints to be specified by the caller.
                scannerHandler.decodeHints = DecodeHintManager.parseDecodeHints(inputUri)
            }
            scannerHandler.characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET)
        }

        mBinding.vfvScan.setCameraManager(scannerHandler.getCameraManager())
    }

    /**
     * 是否是ZXing的Url
     */
    private fun isZXingURL(dataString: String?): Boolean {
        if (dataString == null) {
            return false
        }
        for (url in ZXING_URLS) {
            if (dataString.startsWith(url)) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        //TODO 监听生命周期
        scannerHandler.resume(mBinding.svCamera.holder)
    }

    override fun onPause() {
        super.onPause()
        scannerHandler.pause()
    }

}
