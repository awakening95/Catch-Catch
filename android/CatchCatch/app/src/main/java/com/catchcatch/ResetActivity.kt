package com.catchcatch

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference
import java.net.Socket

class ResetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset)

        Reset(this).execute()
    }

    class Reset(context: ResetActivity) : AsyncTask<String, String, String>() {
        private val activityReference: WeakReference<ResetActivity> = WeakReference(context)

        private val ip = "15.164.75.141" // Server IP
        private val port = 9601 // Server Port

        private val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
        private val id = prefs.getString("id", "")!!

        override fun doInBackground(vararg info: String?): String {
            try {
                val socket = Socket(ip, port)
                val outStream = socket.outputStream
                val inputStream = socket.inputStream

                val data = "$id reset "
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
                    Toast.makeText(activity, "초기화를 완료하였습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "초기화에 실패하였습니다.\n잠시 후에 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }

                val intent = Intent(activity, SettingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                activity.startActivity(intent)
                activity.finish()
            }
        }
    }
}
