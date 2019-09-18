package com.yifan.scanner

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.yifan.scanner.lib.camera.CameraManager
import com.yifan.scanner.lib.camera.CaptureActivityHandler
import java.io.IOException
import java.util.*

class Scanner(
    private val context: Context,
    private val view: ViewfinderView
) : ScannerHandler,
    SurfaceHolder.Callback {

    var hasSurface = false

    private var cameraManager: CameraManager

    lateinit var handler: CaptureActivityHandler

    var savedResultToShow: Result? = null

    var decodeFormats: EnumSet<BarcodeFormat>? = null
    var decodeHints: EnumMap<DecodeHintType, Any>? = null
    var characterSet: String? = null

    init {
        cameraManager = CameraManager(context)
    }

    companion object {
        val TAG = "Scanner"
    }

    override fun getCameraManager(): CameraManager {
        return cameraManager;
    }

    override fun getHandler(): Handler {
        return handler
    }

    override fun getContext(): Context {
        return context
    }

    override fun getPackageManager(): PackageManager {
        return getPackageManager()
    }

    override fun setResult(resultCode: Int, intent: Intent) {
        if (context is AppCompatActivity) {
            context.setResult(resultCode, intent)
        }
    }

    override fun finish() {
        if (context is AppCompatActivity) {
            context.finish()
        }
    }

    override fun handleDecode(rawResult: Result, barcode: Bitmap?, scaleFactor: Float) {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        Log.i(TAG, "handleDecode: " + rawResult.text)
    }

    override fun startActivity(intent: Intent) {
        if (context is AppCompatActivity) {
            context.startActivity(intent)
        }

    }

    override fun getViewfinderView(): ViewfinderView {
        return view
    }

    override fun drawViewfinder() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun surfaceChanged(holder: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        hasSurface = false
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (null == holder) {
            return
        }
        //等待surfaceview初始化完成之后，再初始化相机或设置回调
        if (!hasSurface) {
            hasSurface = true
            initCamera(holder, characterSet, decodeFormats, decodeHints)
        }
    }

    fun resume(surfaceHolder: SurfaceHolder) {
        if (hasSurface) {
            initCamera(surfaceHolder, characterSet, decodeFormats, decodeHints)
        } else {
            surfaceHolder?.addCallback(this)
            surfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
    }


    fun initCamera(
        surfaceHolder: SurfaceHolder,
        characterSet: String?,
        decodeFormats: EnumSet<BarcodeFormat>?,
        decodeHints: MutableMap<DecodeHintType, Any>?
    ) {
        if (surfaceHolder == null) {
            throw IllegalStateException("No SurfaceHolder provided")
        }
        if (cameraManager.isOpen) {
            Log.w(
                AbstractScannerActivity.TAG,
                "initCamera() while already open -- late SurfaceView callback?"
            )
            return
        }

        try {
            cameraManager.openDriver(surfaceHolder)
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            handler = CaptureActivityHandler(
                this,
                decodeFormats,
                decodeHints,
                characterSet!!,
                cameraManager
            )
            decodeOrStoreSavedBitmap(null, null)
        } catch (ioe: IOException) {
            Log.w(AbstractScannerActivity.TAG, ioe)
            displayFrameworkBugMessageAndExit()
        } catch (e: RuntimeException) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(AbstractScannerActivity.TAG, "Unexpected error initializing camera", e)
            if (BuildConfig.DEBUG) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
            displayFrameworkBugMessageAndExit()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun displayFrameworkBugMessageAndExit() {
        if (context is Activity) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(AbstractScannerActivity.TAG)
            builder.setMessage(context.getString(R.string.msg_camera_framework_bug))
            builder.setPositiveButton(R.string.button_ok, FinishListener(context))
            builder.setOnCancelListener(FinishListener(context))
            builder.show()
        }
    }


    fun decodeOrStoreSavedBitmap(bitmap: Bitmap?, result: Result?) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result
        } else {
            if (result != null) {
                savedResultToShow = result
            }
            if (savedResultToShow != null) {
                val message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow)
                handler.sendMessage(message)
            }
            savedResultToShow = null
        }
    }

}