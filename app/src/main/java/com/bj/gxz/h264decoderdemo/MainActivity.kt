package com.bj.gxz.h264decoderdemo

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaCodecList
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    // 抽取aac音频文件
    // ffmpeg -i douyin.MP4 -acodec copy -vn  douyin.aac
    // 抽取H264视频文件
    // ffmpeg -i douyin.MP4  -c:v copy -bsf:v h264_mp4toannexb -an  douyin.h264


    private val TAG: String = "H264"
    private val width = 720
    private val height = 1280
    private lateinit var path: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.e(TAG, "displayMetrics" + resources.displayMetrics.toString())

        path = Environment.getExternalStorageDirectory().absolutePath + "/douyin.h264"

        checkPermission()
        initSurface()
        getSupportCodec()
    }


    private fun checkPermission() {
        // 简单处理下权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            requestPermissions(permissions, 1)
        }
    }

    private lateinit var holder: SurfaceHolder
    private fun initSurface() {
        id_sf.holder.addCallback(object : Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.e(TAG, "surfaceCreated")
                this@MainActivity.holder = holder
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {

            }
        })
    }

    fun play(view: View) {
        H264Decoder(path, width, height, holder.surface).start()
//        H264DecoderISaveImage(path, width, height, holder.surface).start()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getSupportCodec() {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codecs = list.codecInfos
        Log.d(TAG, "Decoders:")
        for (codec in codecs) {
            if (!codec.isEncoder) Log.d(TAG, codec.name)
        }
        Log.d(TAG, "Encoders:")
        for (codec in codecs) {
            if (codec.isEncoder) Log.d(TAG, codec.name)
        }
    }
}

