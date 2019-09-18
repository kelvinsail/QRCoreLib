package com.yifan.scanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class AbstractScannerActivity : AppCompatActivity(){

    companion object {
        val TAG = "AbstractScannerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


}