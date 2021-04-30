package com.bj.gxz.h264decoderdemo

import android.graphics.*
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Environment
import android.util.Log
import android.view.Surface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by guxiuzhong on 2020/12/19.
 */
class H264DecoderISaveImage(
    path: String,
    var width: Int,
    var height: Int,
    surface: Surface,
) : Thread() {
    private val TAG = "H264"
    var bytes: ByteArray? = null

    var mediaCodec: MediaCodec
    var saveImage: Long = 0


    init {
        // demo测试，一次性读取
        bytes = FileUtil.getBytes(path)
        // video/avc就是H264，创建解码器
        mediaCodec = MediaCodec.createDecoderByType("video/avc")
        val mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        // 不再渲染到surface上
        mediaCodec.configure(mediaFormat, null, null, 0)
    }

    override fun run() {
        mediaCodec.start()
        decode()
    }

    private fun decode() {
        if (bytes == null) {
            return
        }
        var startFrameIndex = 0
        val totalSizeIndex = bytes!!.size - 1
        Log.i(TAG, "totalSizeIndex=$totalSizeIndex")
        val inputBuffers = mediaCodec.inputBuffers
        val info = MediaCodec.BufferInfo()
        while (true) {
            // 1ms=1000us 微妙
            val inIndex = mediaCodec.dequeueInputBuffer(10_000)
            if (inIndex >= 0) {
                // 分割出一帧数据
                if (totalSizeIndex == 0 || startFrameIndex >= totalSizeIndex) {
                    Log.e(TAG, "startIndex >= totalSize ,break")
                    break
                }
                val nextFrameStart: Int =
                    findNextFrame(bytes!!, startFrameIndex + 1, totalSizeIndex)
                if (nextFrameStart == -1) {
                    Log.e(TAG, "nextFrameStart==-1 break")
                    break
                }
                // 填充数据
                val byteBuffer = inputBuffers[inIndex]
                byteBuffer.clear()
                byteBuffer.put(bytes!!, startFrameIndex, nextFrameStart - startFrameIndex)

                mediaCodec.queueInputBuffer(inIndex, 0, nextFrameStart - startFrameIndex, 0, 0)

                startFrameIndex = nextFrameStart

            } else {
                continue
            }
            var outIndex = mediaCodec.dequeueOutputBuffer(info, 10_000)
            while (outIndex >= 0) {
                // 3s 保存一张图片
                if (System.currentTimeMillis() - saveImage > 3000) {
                    saveImage = System.currentTimeMillis()

                    val byteBuffer: ByteBuffer = mediaCodec.outputBuffers[outIndex]
                    byteBuffer.position(info.offset)
                    byteBuffer.limit(info.offset + info.size)
                    val ba = ByteArray(byteBuffer.remaining())
                    byteBuffer.get(ba)

                    try {
                        val parent =
                            File(Environment.getExternalStorageDirectory().absolutePath + "/h264pic/")
                        if (!parent.exists()) {
                            parent.mkdirs()
                            Log.d(TAG, "parent=${parent.absolutePath}")
                        }

                        // 将NV21格式图片，以质量70压缩成Jpeg
                        val path = "${parent.absolutePath}/${System.currentTimeMillis()}-frame.jpg"
                        Log.e(TAG, "path:$path")
                        val fos = FileOutputStream(File(path))
                        val yuvImage = YuvImage(ba, ImageFormat.NV21, width, height, null)
                        yuvImage.compressToJpeg(
                            Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()),
                            80, fos)
                        fos.flush()
                        fos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                // 这里用简单的时间方式保持视频的fps，不然视频会播放很快
                // demo 的MP4是30fps
                try {
                    sleep(33)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                // 参数2 渲染到surface上，surface就是mediaCodec.configure的参数2
                mediaCodec.releaseOutputBuffer(outIndex, false)
                outIndex = mediaCodec.dequeueOutputBuffer(info, 0)

            }
        }
        mediaCodec.stop()
        mediaCodec.release()
    }

    private fun findNextFrame(bytes: ByteArray, startIndex: Int, totalSizeIndex: Int): Int {
        for (i in startIndex..totalSizeIndex) {
            // 00 00 00 01 H264的启始码
            if (bytes[i].toInt() == 0x00 && bytes[i + 1].toInt() == 0x00 && bytes[i + 2].toInt() == 0x00 && bytes[i + 3].toInt() == 0x01) {
                Log.d(TAG, "bytes[i+4]=0X${Integer.toHexString(bytes[i + 4].toInt())}")
                Log.d(TAG, "bytes[i+4]=${(bytes[i + 4].toInt().and(0X1F))}")
                return i
                // 00 00 01 H264的启始码
            } else if (bytes[i].toInt() == 0x00 && bytes[i + 1].toInt() == 0x00 && bytes[i + 2].toInt() == 0x01) {
                Log.d(TAG, "bytes[i+3]=0X${Integer.toHexString(bytes[i + 3].toInt())}")
                Log.d(TAG, "bytes[i+3]=${(bytes[i + 3].toInt().and(0X1F))}")
                return i
            }
        }
        return -1
    }
}
