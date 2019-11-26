package com.catchcatch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_calibration.*
import java.lang.ref.WeakReference
import java.net.Socket
import android.widget.ImageView
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import android.content.DialogInterface
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.appcompat.app.AlertDialog


class CalibrationActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("화면 조정")
        builder.setMessage("영상에서 보이는 프로젝터 화면의 오른쪽 위 꼭지점과 왼쪽 아래 꼭지점을 차례대로 찍어주세요")
        builder.setPositiveButton("확인") { dialog, which -> }
        builder.show()

        val img1 = ImageView(this)
        img1.setImageResource(R.drawable.dot)

        val img2 = ImageView(this)
        img2.setImageResource(R.drawable.dot)

        var x1 = 0.0f
        var y1 = 0.0f
        var x2 = 0.0f
        var y2 = 0.0f
        var step = 0

        activity_calibration_canvas.setOnTouchListener { v, event ->
            val action = event.action
            when(action){
                MotionEvent.ACTION_DOWN -> {
                    if (step == 0) {
                        x1 = event.x
                        y1 = event.y

                        img1.x = x1 -960.0f
                        img1.y = y1
                        Log.d("test", "${img1.x}, ${img1.y}")
                        activity_calibration_canvas.addView(img1)

                        step += 1

                    } else if (step == 1) {
                        x2 = event.x
                        y2 = event.y

                        img2.x = x2 -960.0f
                        img2.y = y2
                        Log.d("test", "${img2.x}, ${img2.y}")
                        activity_calibration_canvas.addView(img2)

                        step += 1

                    } else if (step == 2) {
                        Toast.makeText(this, "초기화 또는 설정을 완료해주십오.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            true
        }

        activity_calibration_cancle.setOnClickListener {
            activity_calibration_canvas.removeView(img1)
            activity_calibration_canvas.removeView(img2)
            step = 0
        }

        activity_calibration_check.setOnClickListener {
            if (step == 2) {
                val convertLocationX0 = ((img1.x + 960.0f) / 1.5f).toInt().toString()
                val convertLocationY0 = (img1.y / 1.5f).toInt().toString()
                val convertLocationX1 = ((img2.x + 960.0f) / 1.5f).toInt().toString()
                val convertLocationY1 = (img2.y / 1.5f).toInt().toString()
                Calibration(this).execute(convertLocationX0, convertLocationY0, convertLocationX1, convertLocationY1)
            } else {
                Toast.makeText(this, "영상에서 보이는 프로젝터 화면의 오른쪽 위 꼭지점과 왼쪽 아래 꼭지점을 차례대로 찍어주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = this.getSharedPreferences("info", Context.MODE_PRIVATE)
        val id = prefs.getString("id", "")!!
        val replacedId = id.replace("@", "_").replace(".", "_")

        val dataSourceFactory = DefaultHttpDataSourceFactory(Util.getUserAgent(this, "CatchCatch"))
        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse("http://15.164.75.141:8080/hls/${replacedId}_streaming.m3u8"))
        player = ExoPlayerFactory.newSimpleInstance(this)
        player!!.prepare(hlsMediaSource)
        player!!.playWhenReady = true
        player!!.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException?) {
                Log.d("onPlayerError", "$error")
                player!!.prepare(hlsMediaSource)
            }
        })
        activity_calibration_exoplayer_view.player = player
    }

    override fun onPause() {
        super.onPause()

        if (player != null) {
            activity_calibration_exoplayer_view.player = null
            player!!.release()
        }
    }

    class Calibration(context: CalibrationActivity) : AsyncTask<String, String, String>() {
        private val activityReference: WeakReference<CalibrationActivity> = WeakReference(context)

        private val ip = "15.164.75.141" // Server IP
        private val port = 9601 // Server Port

        private val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
        private val id = prefs.getString("id", "")!!

        override fun onPreExecute() {
            super.onPreExecute()

            val activity = activityReference.get()
            if (activity != null) {
                activity.activity_calibration_start_layout.visibility = View.INVISIBLE
                activity.activity_calibration_progress.visibility = View.VISIBLE
            }
        }

        override fun doInBackground(vararg info: String?): String {
            try {
                val x0 = info[0]
                val y0 = info[1]
                val x1 = info[2]
                val y1 = info[3]

                val socket = Socket(ip, port)
                val outStream = socket.outputStream
                val inputStream = socket.inputStream

                val data = "$id calibration $x0 $y0 $x1 $y1 "
                outStream.write(data.toByteArray())

                val dataArr = ByteArray(1)
                inputStream.read(dataArr)
                val recvData = String(dataArr)

                socket.close()

                return recvData

            } catch (e: Exception) {

                return "0"
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            val activity = activityReference.get()

            if (activity != null) {
                if (result == "1") {
                    Toast.makeText(activity, "설정을 완료하였습니다.", Toast.LENGTH_SHORT).show()

                    val intent = Intent(activity, SettingActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    activity.startActivity(intent)
                    activity.finish()
                } else {
                    Toast.makeText(activity, "설정을 실패하였습니다.\n잠시 후에 다시 시도해주세요.", Toast.LENGTH_SHORT).show()

                    val intent = Intent(activity, CalibrationActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    activity.startActivity(intent)
                    activity.finish()
                }
            }
        }
    }
}
