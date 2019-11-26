package com.catchcatch

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRecorder
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_operation.*
import org.jetbrains.anko.downloadManager
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class StreamingActivity: AppCompatActivity() {

    lateinit var mediaRecorder: MediaRecorder
    private var StreamingRecordingTask: AsyncTask<String, String, String>? = null
    private var voiceSendTask: AsyncTask<String, Void, String>? = null

    private var id: String? = null
    private var player: SimpleExoPlayer? = null
    private var downloadID: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operation)

        activity_operation_constraint_layout_manual_keypad.visibility = View.INVISIBLE
        activity_operation_constraint_layout_auto.visibility = View.INVISIBLE

        activity_operation_button_power_off_auto.visibility = View.INVISIBLE
        activity_operation_button_option_auto.visibility = View.INVISIBLE
        activity_operation_button_manual_auto.visibility = View.INVISIBLE

        val prefs = this.getSharedPreferences("info", Context.MODE_PRIVATE)
        id = prefs.getString("id", "")!!
        val serverIP = prefs.getString("server_ip", "")!!
        downloadID = prefs.getLong("download_id", -1)

        val output = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}" + "/recording.mp3" // 녹음 파일 저장 경로
        val replacedId = id!!.replace("@", "_").replace(".", "_")

        CheckCatchCatchModuleNetworkState(this).execute()

        val query = DownloadManager.Query()
        val cursor = downloadManager.query(query)
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            do {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                if ((status == 1) || (status == 2)) {
                    val uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))
                    if (uri == "http://${serverIP}/stream/${replacedId}_streaming.flv") {
                        activity_operation_button_video_record_wait_auto.visibility = View.INVISIBLE
                        activity_operation_button_video_record_download_auto.visibility = View.VISIBLE
                        activity_operation_button_video_record_stop_auto.visibility = View.INVISIBLE
                        activity_operation_button_video_record_run_auto.visibility = View.INVISIBLE
                    }
                }
            } while (cursor.moveToNext())
        }

        /*-----------------------------------Auto-----------------------------------*/
        // 녹화 시작 버튼 클릭
        activity_operation_button_video_record_run_auto.setOnClickListener {
            StreamingRecordingTask = StreamingRecording(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

        // 녹화 중지 버튼 클릭
        activity_operation_button_video_record_stop_auto.setOnClickListener {
            StreamingDownloading(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

        // 녹음 버튼 클릭
        activity_operation_button_voice_record_run_auto.setOnClickListener {
            mediaRecorder = MediaRecorder()

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setOutputFile(output)

            try {
                mediaRecorder.prepare()
                mediaRecorder.start()

                activity_operation_button_voice_record_wait_auto.visibility = View.INVISIBLE
                activity_operation_button_voice_record_run_auto.visibility = View.INVISIBLE
                activity_operation_button_voice_record_stop_auto.visibility = View.VISIBLE

            } catch (e: Exception) {
                Log.d("StreamingActivity", "$e")
            }
        }

        // 녹음 중지 버튼 클릭
        activity_operation_button_voice_record_stop_auto.setOnClickListener {
            mediaRecorder.stop()
            mediaRecorder.release()

            activity_operation_button_voice_record_wait_auto.visibility = View.VISIBLE
            activity_operation_button_voice_record_run_auto.visibility = View.INVISIBLE
            activity_operation_button_voice_record_stop_auto.visibility = View.INVISIBLE

            voiceSendTask = VoiceSend(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
        activity_operation_exoplayer_view.player = player

        registerReceiver(broadCastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onPause() {
        super.onPause()

        if (player != null) {
            activity_operation_exoplayer_view.player = null
            player!!.release()
        }

        if (StreamingRecordingTask?.status == AsyncTask.Status.RUNNING) {
            StreamingRecordingTask!!.cancel(true)
        }

        if (voiceSendTask?.status == AsyncTask.Status.RUNNING) {
            voiceSendTask!!.cancel(true)
        }

        unregisterReceiver(broadCastReceiver)
    }

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (id == downloadID) { // 알림
                Toast.makeText(applicationContext, "다운로드를 완료하였습니다.", Toast.LENGTH_SHORT).show()

                activity_operation_button_video_record_wait_auto.visibility = View.INVISIBLE
                activity_operation_button_video_record_download_auto.visibility = View.INVISIBLE
                activity_operation_button_video_record_stop_auto.visibility = View.INVISIBLE
                activity_operation_button_video_record_run_auto.visibility = View.VISIBLE
            }
        }
    }

    class CheckCatchCatchModuleNetworkState(context: StreamingActivity) : AsyncTask<String, String, String>() {
        private val activityReference: WeakReference<StreamingActivity> = WeakReference(context)

        private var tag: String = "CheckCatchCatchModuleNetworkState"

        private val ip = "15.164.75.141" // Server IP
        private val port = 9600 // Server Port

        private val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
        private val id = prefs.getString("id", "")!!

        override fun doInBackground(vararg info: String): String {
            try {
                val socket = Socket(ip, port)
                val outStream = socket.outputStream
                val inputStream = socket.inputStream

                outStream.write(id.toByteArray())

                val dataArr = ByteArray(1)
                inputStream.read(dataArr)
                val recvData = String(dataArr)

                socket.close()

                return recvData

            } catch (e: java.net.ConnectException) {
                Log.d(tag, "$e")
                return "2"
            }
        }
        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            val activity = activityReference.get()

            if (result == "0") {
                if (activity?.activity_operation_constraint_layout != null) {
                    activity.activity_operation_constraint_layout_server_not_connect.visibility = View.INVISIBLE
                    activity.activity_operation_constraint_layout_client_not_connect.visibility = View.VISIBLE
                    activity.activity_operation_constraint_layout_auto.visibility = View.INVISIBLE
                    activity.activity_operation_exoplayer_view.visibility = View.INVISIBLE
                }
            } else if (result == "1") {
                if (activity?.activity_operation_constraint_layout != null) {
                    activity.activity_operation_constraint_layout_server_not_connect.visibility = View.INVISIBLE
                    activity.activity_operation_constraint_layout_client_not_connect.visibility = View.INVISIBLE
                    activity.activity_operation_constraint_layout_auto.visibility = View.VISIBLE
                    activity.activity_operation_exoplayer_view.visibility = View.VISIBLE
                }
            }
            else if (result == "2") {
                if (activity?.activity_operation_constraint_layout != null) {
                    activity.activity_operation_constraint_layout_server_not_connect.visibility = View.VISIBLE
                    activity.activity_operation_constraint_layout_client_not_connect.visibility = View.INVISIBLE
                    activity.activity_operation_constraint_layout_auto.visibility = View.INVISIBLE
                    activity.activity_operation_exoplayer_view.visibility = View.INVISIBLE
                }
            }
        }
    }


    // 녹화 시작
    class StreamingRecording(context: StreamingActivity) : AsyncTask<String, String, String>() {
        private val activityReference: WeakReference<StreamingActivity> = WeakReference(context)

        private val ip = "15.164.75.141" // Server IP
        private val port = 9601 // Server Port

        private val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
        private val id = prefs.getString("id", "")!!

        override fun onPreExecute() {
            super.onPreExecute()

            val activity = activityReference.get()

            if (activity != null) {
                activity.activity_operation_button_video_record_wait_auto.visibility = View.VISIBLE
                activity.activity_operation_button_video_record_wait_manual_joystick.visibility = View.VISIBLE
                activity.activity_operation_button_video_record_wait_manual_keypad.visibility = View.VISIBLE

                activity.activity_operation_button_video_record_download_auto.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_download_manual_joystick.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_download_manual_keypad.visibility = View.INVISIBLE

                activity.activity_operation_button_video_record_stop_auto.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_stop_manual_joystick.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_stop_manual_keypad.visibility = View.INVISIBLE

                activity.activity_operation_button_video_record_run_auto.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_run_manual_joystick.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_run_manual_keypad.visibility = View.INVISIBLE
            }
        }

        override fun doInBackground(vararg info: String?): String {
            val socket = Socket(ip, port)
            val outStream = socket.outputStream
            val inputStream = socket.inputStream

            val data = "$id streaming_recording "
            outStream.write(data.toByteArray())

            val dataArr = ByteArray(1)
            inputStream.read(dataArr)
            val recvData = String(dataArr)

            if (recvData == "1") { // CatchCatch 모듈에서 녹화 준비 완료
                publishProgress() // Toast로 "녹화가 시작되었습니다." 표시
            }

            socket.close()
            return recvData
        }

        override fun onProgressUpdate(vararg num: String) {
            val activity = activityReference.get()

            if (activity != null) {
                Toast.makeText(activity, "녹화가 시작되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            val activity = activityReference.get()

            if (activity != null) {
                if (result == "1") {
                    activity.activity_operation_button_video_record_wait_auto.visibility = View.INVISIBLE
                    activity.activity_operation_button_video_record_download_auto.visibility = View.INVISIBLE
                    activity.activity_operation_button_video_record_stop_auto.visibility = View.VISIBLE
                    activity.activity_operation_button_video_record_run_auto.visibility = View.INVISIBLE

                } else {
                    activity.activity_operation_button_video_record_wait_auto.visibility = View.INVISIBLE
                    activity.activity_operation_button_video_record_download_auto.visibility = View.INVISIBLE
                    activity.activity_operation_button_video_record_stop_auto.visibility = View.INVISIBLE
                    activity.activity_operation_button_video_record_run_auto.visibility = View.VISIBLE

                    Toast.makeText(activity, "녹화에 실패하였습니다. 잠시 후에 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // 녹화 종료
    class StreamingDownloading(context: StreamingActivity) : AsyncTask<String, String, String>() {
        private val activityReference: WeakReference<StreamingActivity> = WeakReference(context)

        private val ip = "15.164.75.141" // Server IP
        private val port = 9601 // Server Port

        private val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
        private val editor = prefs.edit()
        private val id = prefs.getString("id", "")!!

        override fun onPreExecute() {
            super.onPreExecute()

            val activity = activityReference.get()

            if (activity != null) {
                activity.activity_operation_button_video_record_wait_auto.visibility = View.VISIBLE
                activity.activity_operation_button_video_record_wait_manual_joystick.visibility = View.VISIBLE
                activity.activity_operation_button_video_record_wait_manual_keypad.visibility = View.VISIBLE

                activity.activity_operation_button_video_record_download_auto.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_download_manual_joystick.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_download_manual_keypad.visibility = View.INVISIBLE

                activity.activity_operation_button_video_record_stop_auto.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_stop_manual_joystick.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_stop_manual_keypad.visibility = View.INVISIBLE

                activity.activity_operation_button_video_record_run_auto.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_run_manual_joystick.visibility = View.INVISIBLE
                activity.activity_operation_button_video_record_run_manual_keypad.visibility = View.INVISIBLE
            }
        }

        override fun doInBackground(vararg info: String?): String {
            val socket = Socket(ip, port)
            val outStream = socket.outputStream
            val inputStream = socket.inputStream

            val data = "$id streaming_downloading "
            outStream.write(data.toByteArray())

            val dataArr = ByteArray(1)
            inputStream.read(dataArr)
            val recvData = String(dataArr)

            socket.close()
            return recvData
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            val activity = activityReference.get()
            val replacedId = id.replace("@", "_").replace(".", "_")

            if (activity != null) {
                if (result == "1") {
                    val builder = AlertDialog.Builder(activity)
                    builder.setTitle("다운로드 준비 완료")
                    builder.setMessage("다운로드 하시겠습니까?")
                    builder.setPositiveButton("예") { dialog, which ->
                        activity.activity_operation_button_video_record_wait_auto.visibility = View.INVISIBLE
                        activity.activity_operation_button_video_record_download_auto.visibility = View.VISIBLE
                        activity.activity_operation_button_video_record_stop_auto.visibility = View.INVISIBLE
                        activity.activity_operation_button_video_record_run_auto.visibility = View.INVISIBLE

                        val catchCatchFolder = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}" + File.separator + "CatchCatch" + File.separator
                        val directory = File(catchCatchFolder)
                        if (!directory.exists()) directory.mkdirs()

                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
                        val fileName = "$timestamp.flv"

                        val file = File(catchCatchFolder + fileName)

                        val request = DownloadManager.Request(Uri.parse("http://${ip}/stream/${replacedId}_streaming_back.flv"))
                            .setTitle("CatchCatch")
                            .setDescription("다운로드 중 입니다...")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationUri(Uri.fromFile(file))
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)

                        activity.downloadID = activity.downloadManager.enqueue(request)
                        editor.putLong("download_id", activity.downloadID!!).apply()
                    }

                    builder.setNegativeButton("아니오") { dialog, which ->
                        activity.activity_operation_button_video_record_wait_auto.visibility = View.INVISIBLE
                        activity.activity_operation_button_video_record_download_auto.visibility = View.INVISIBLE
                        activity.activity_operation_button_video_record_stop_auto.visibility = View.INVISIBLE
                        activity.activity_operation_button_video_record_run_auto.visibility = View.VISIBLE
                    }

                    builder.show()
                } else {
                    activity.activity_operation_button_video_record_wait_auto.visibility = View.INVISIBLE
                    activity.activity_operation_button_video_record_download_auto.visibility = View.INVISIBLE
                    activity.activity_operation_button_video_record_stop_auto.visibility = View.VISIBLE
                    activity.activity_operation_button_video_record_run_auto.visibility = View.INVISIBLE

                    Toast.makeText(activity, "다운로드에 실패하였습니다. 잠시 후에 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    class VoiceSend(context: StreamingActivity) : AsyncTask<String, Void, String>() {
        private val activityReference: WeakReference<StreamingActivity> = WeakReference(context)

        private val ip = "15.164.75.141" // Server IP
        private val port = 9602 // Server Port

        private val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
        private val id = prefs.getString("id", "")!!

        override fun doInBackground(vararg p0: String?): String {
            val file = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}" + "/recording.mp3"
            val fileInputStream = FileInputStream(file)

            val buffer = ByteArray(1024)
            var readBytes = fileInputStream.read(buffer)

            val socket = Socket(ip, port)
            val outputStream = socket.outputStream
            val inputStream = socket.inputStream

            outputStream.write(id.toByteArray())

            val checkBuffer = ByteArray(1)
            inputStream.read(checkBuffer)
            val recvData = String(checkBuffer)

            if (recvData != "1") {
                return "0"
            }

            var totalReadBytes = 0L
            val fileSize = File(file).length()

            while (readBytes > 0) {
                totalReadBytes += readBytes

                if (totalReadBytes == fileSize) { // 마지막 패킷을 보낼 때
                    outputStream.write(buffer, 0, readBytes)
                    outputStream.write("CatchCatch".toByteArray(), 0, "CatchCatch".toByteArray().size)
                } else {
                    outputStream.write(buffer, 0, readBytes)
                }

                readBytes = fileInputStream.read(buffer)
            }

            if (totalReadBytes != fileSize) { // 오류로 인해 데이터를 완전히 못 보냈을 경우
                outputStream.write("ErrorError".toByteArray(), 0, "ErrorError".toByteArray().size)
            }

            fileInputStream.close()
            outputStream.close()
            socket.close()

            return "1"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            val activity = activityReference.get()

            if (result == "1") Toast.makeText(activity, "녹음 전송에 성공하였습니다.", Toast.LENGTH_SHORT).show()
            else Toast.makeText(activity, "녹음 전송에 실패하였습니다.", Toast.LENGTH_SHORT).show()

            if (activity != null) {
                activity.activity_operation_button_voice_record_wait_auto.visibility = View.INVISIBLE
                activity.activity_operation_button_voice_record_run_auto.visibility = View.VISIBLE
                activity.activity_operation_button_voice_record_stop_auto.visibility = View.INVISIBLE
            }
        }
    }
}