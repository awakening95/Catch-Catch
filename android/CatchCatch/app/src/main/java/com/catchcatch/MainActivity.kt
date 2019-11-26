package com.catchcatch

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_operation.*
import kotlinx.android.synthetic.main.views.*
import kotlinx.android.synthetic.main.views.view.*
import java.io.File
import java.net.Socket
import java.util.concurrent.ThreadPoolExecutor
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = this.getSharedPreferences("info", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val videoRecordState = prefs.getString("video_record_state", "0")!! // 0: 대기 중, 1: 녹화 중, 2: 다운로드 준비 중, 3: 다운로드 중
        var runningGame = prefs.getString("running_game", "-1")!! // -1: 실행 중인 게임 없음, 0: 0번 게임 실행 중, 1: 1번 게임 실행 중.....

        if (videoRecordState == "2") { // 시작할 때 녹화 상태가 다운로드 중일 때 초기화
            editor.putString("recording_state", "0").apply()
        }

        val permission = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION"
        )
        val permissionRequestCode = 1
        ActivityCompat.requestPermissions(this, permission, permissionRequestCode)

        var index = 1

        if (runningGame == "0") {
            v1_txt.text = "실행 중"
        }

        v4_img.visibility = View.GONE

        v1_img.setOnClickListener {
            runningGame = prefs.getString("running_game", "-1")!!

            if (runningGame == "0") {
                v1_txt.text = "실행 중"
            } else {
                v1_txt.text = "시작하기"
            }

            if (index == 2) {
                activity_main_text_view.text = "Catch Rat"

                v2_txt.visibility = View.GONE
                v1_txt.visibility = View.VISIBLE

                v4_img.visibility = View.GONE
                v6_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s2, R.id.s1)
                activity_main_motion_layout.transitionToEnd()

                index = 1

            } else if (index == 6) {
                activity_main_text_view.text = "Catch Rat"

                v6_txt.visibility = View.GONE
                v1_txt.visibility = View.VISIBLE

                v4_img.visibility = View.GONE
                v2_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s6, R.id.s1)
                activity_main_motion_layout.transitionToEnd()

                index = 1

            } else if (index == 1) { // Game0
                if (runningGame != "0") { // 선택한 게임이 실행 중이지 않을 때, 선택한 게임 실행
                    GameRun(this).execute("0") // 0번 게임 실행
                    v1_txt.text = "실행 중"
                }

                val intent = Intent(this, OperationActivity::class.java)
                intent.putExtra("game_type", "joystick")
                startActivityForResult(intent, 9000)
            }
        }

        v2_img.setOnClickListener {
            runningGame = prefs.getString("running_game", "-1")!!

            if (runningGame == "1") {
                v2_txt.text = "실행 중"
            } else {
                v2_txt.text = "시작하기"
            }

            if (index == 1) {
                activity_main_text_view.text = "Catch Fish"

                v1_txt.visibility = View.GONE
                v2_txt.visibility = View.VISIBLE

                v5_img.visibility = View.GONE
                v3_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s1, R.id.s2)
                activity_main_motion_layout.transitionToEnd()

                index = 2

            } else if (index == 3) {
                activity_main_text_view.text = "Catch Fish"

                v3_txt.visibility = View.GONE
                v2_txt.visibility = View.VISIBLE

                v5_img.visibility = View.GONE
                v1_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s3, R.id.s2)
                activity_main_motion_layout.transitionToEnd()

                index = 2

            } else if (index == 2) { // Game1
                if (runningGame != "1") {
                    GameRun(this).execute("1")
                    v2_txt.text = "실행 중"
                }

                val intent = Intent(this, OperationActivity::class.java)
                intent.putExtra("game_type", "joystick")
                startActivityForResult(intent, 9000)
            }
        }

        v3_img.setOnClickListener {
            runningGame = prefs.getString("running_game", "-1")!!

            if (runningGame == "2") {
                v3_txt.text = "실행 중"
            } else {
                v3_txt.text = "시작하기"
            }

            if (index == 2) {
                activity_main_text_view.text = "Catch Mole"

                v2_txt.visibility = View.GONE
                v3_txt.visibility = View.VISIBLE

                v6_img.visibility = View.GONE
                v4_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s2, R.id.s3)
                activity_main_motion_layout.transitionToEnd()

                index = 3

            } else if (index == 4) {
                activity_main_text_view.text = "Catch Mole"

                v4_txt.visibility = View.GONE
                v3_txt.visibility = View.VISIBLE

                v6_img.visibility = View.GONE
                v2_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s4, R.id.s3)
                activity_main_motion_layout.transitionToEnd()

                index = 3

            } else if (index == 3) { // Game2
                if (runningGame != "2") {
                    GameRun(this).execute("2")
                    v3_txt.text = "실행 중"
                }

                val intent = Intent(this, OperationActivity::class.java)
                intent.putExtra("game_type", "keypad")
                startActivityForResult(intent, 9000)
            }
        }

        v4_img.setOnClickListener {
            if (index == 3) {
                activity_main_text_view.text = "지켜보기"

                v3_txt.visibility = View.GONE
                v4_txt.visibility = View.VISIBLE

                v1_img.visibility = View.GONE
                v5_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s3, R.id.s4)
                activity_main_motion_layout.transitionToEnd()

                index = 4

            } else if (index == 5) {
                activity_main_text_view.text = "지켜보기"

                v5_txt.visibility = View.GONE
                v4_txt.visibility = View.VISIBLE

                v1_img.visibility = View.GONE
                v3_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s5, R.id.s4)
                activity_main_motion_layout.transitionToEnd()

                index = 4

            } else if (index == 4) { // 지켜보기
                val intent = Intent(this, StreamingActivity::class.java)
                startActivity(intent)
            }
        }

        v5_img.setOnClickListener {
            if (index == 4) {
                activity_main_text_view.text = "보관함"

                v4_txt.visibility = View.GONE
                v5_txt.visibility = View.VISIBLE

                v2_img.visibility = View.GONE
                v6_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s4, R.id.s5)
                activity_main_motion_layout.transitionToEnd()

                index = 5

            } else if (index == 6) {
                activity_main_text_view.text = "보관함"

                v6_txt.visibility = View.GONE
                v5_txt.visibility = View.VISIBLE

                v2_img.visibility = View.GONE
                v4_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s6, R.id.s5)
                activity_main_motion_layout.transitionToEnd()

                index = 5

            } else if (index == 5) { // 보관함
                val intent = Intent(this, StorageActivity::class.java)
                startActivity(intent)
            }
        }

        v6_img.setOnClickListener {
            if (index == 5) {
                activity_main_text_view.text = "환경설정"

                v5_txt.visibility = View.GONE
                v6_txt.visibility = View.VISIBLE

                v3_img.visibility = View.GONE
                v1_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s5, R.id.s6)
                activity_main_motion_layout.transitionToEnd()

                index = 6

            } else if (index == 1) {
                activity_main_text_view.text = "환경설정"

                v1_txt.visibility = View.GONE
                v6_txt.visibility = View.VISIBLE

                v3_img.visibility = View.GONE
                v5_img.visibility = View.VISIBLE

                activity_main_motion_layout.setTransition(R.id.s1, R.id.s6)
                activity_main_motion_layout.transitionToEnd()

                index = 6

            } else if (index == 6) { // 환경설정
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 9000) {
            if (resultCode == 1) {
                v1_txt.text = "시작하기"
                v2_txt.text = "시작하기"
                v3_txt.text = "시작하기"
            }
        }
    }

    class GameRun(context: MainActivity) : AsyncTask<String, Void, String>() {
        private val ip = "15.164.75.141" // Server IP
        private val port = 9601 // Server Port

        private val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
        private val editor = prefs.edit()
        private val id = prefs.getString("id", "")!!

        override fun doInBackground(vararg info: String): String {
            try {
                val socket = Socket(ip, port)
                val outStream = socket.outputStream

                val number = info[0]

                val data = "$id game_run $number "

                outStream.write(data.toByteArray())

                socket.close()

                editor.putString("running_game", number).apply()

                return "1"

            } catch (e: Exception) {

                return "0"
            }
        }
    }
}