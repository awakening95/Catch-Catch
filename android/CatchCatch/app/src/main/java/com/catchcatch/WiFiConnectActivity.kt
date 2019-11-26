package com.catchcatch

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_wifi_connect.*
import org.jetbrains.anko.wifiManager
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.Socket

class WiFiConnectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_connect)

        val intent = getIntent()
        val ssid = intent.extras?.getString("ssid")
        val key = intent.extras?.getString("key")

        WiFiConnect(this).execute(ssid, key)
    }

    // Module에 연결할 공유기 정보 및 사용자의 ID 전달 후 서버에서 Module이 접속하는지 확인
    class WiFiConnect(context: WiFiConnectActivity) : AsyncTask<String, String, Boolean>() {
        private val activityReference: WeakReference<WiFiConnectActivity> = WeakReference(context)

        private val ip = "10.10.0.1" // Server IP
        private val port = 9700 // Server Port

        private val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
        private val id = prefs.getString("id", "")!!

        private var netId = -1

        override fun doInBackground(vararg info: String?): Boolean {
            val activity = activityReference.get()

            val ssid = info[0]
            val key = info[1]

            val moduleSSID = "Catch_Catch_2019"
            val moduleKEY = "1234567890"

            val conf = WifiConfiguration()
            conf.SSID = "\"" + moduleSSID + "\""
            conf.status = WifiConfiguration.Status.DISABLED
            conf.priority = 40

            conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)

            conf.preSharedKey = "\"" + moduleKEY + "\""

            if (activity != null) {
                netId = activity.wifiManager.addNetwork(conf)
                activity.wifiManager.disconnect()
                activity.wifiManager.enableNetwork(netId, true)
                activity.wifiManager.reconnect()

                Thread.sleep(5000)

                val ap = "10.10.0.1"
                val cmd = "ping -c 1 -W 10 $ap"

                try {
                    var result = 1

                    for (i in 1..30) {
                        val proc = Runtime.getRuntime().exec(cmd)
                        proc.waitFor()
                        result = proc.exitValue()

                        if (result == 0) {
                            break
                        }
                    }

                    if (result == 0) { // ping 테스트 성공
                        val socket = Socket(ip, port)
                        val outputStream = socket.outputStream
                        val inputStream = socket.inputStream

                        outputStream.write("$ssid $key $id".toByteArray())

                        val checkBuffer = ByteArray(1)
                        inputStream.read(checkBuffer)
                        val recvData = String(checkBuffer)

                        if (recvData == "1") {
                            socket.close()
                            return true

                        } else {
                            socket.close()
                        }
                    }

                } catch (e: Exception) {
                    Log.d("WiFiConnect", "$e")
                }
            }
            return false // 실패, 에러
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)

            val activity = activityReference.get()

            if (activity != null) {
                if (result == true) {
                    activity.activity_wifi_connect_waiting.text = "서비스 준비 중입니다..."
                    activity.activity_wifi_connect_advice.visibility = View.VISIBLE

                    activity.wifiManager.removeNetwork(netId)

                    CheckCatchCatchModuleNetworkState(activity).execute()

                } else {
                    Toast.makeText(activity, "공유기 연결에 실패하였습니다.\n다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(activity, WiFiSearchActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
            }
        }
    }

    class CheckCatchCatchModuleNetworkState(context: WiFiConnectActivity) : AsyncTask<String, Void, String>() {

        private val activityReference: WeakReference<WiFiConnectActivity> = WeakReference(context)

        private var tag: String = "CheckCatchCatchModuleNetworkState"

        private val ip = "15.164.75.141" // Server IP
        private val port = 9600 // Server Port

        private val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
        private val id = prefs.getString("id", "")!!

        override fun doInBackground(vararg info: String): String {
            var result = "0"

            while (true) {
                try {
                    val socket = Socket(ip, port)
                    val outStream = socket.outputStream
                    val inputStream = socket.inputStream

                    outStream.write(id.toByteArray())

                    val dataArr = ByteArray(1)
                    inputStream.read(dataArr)
                    result = String(dataArr)

                    socket.close()

                } catch (e: java.net.ConnectException) {
                    Log.d(tag, "$e")
                    result = "2"
                }

                if (result == "1") {
                    return ""
                }

                Thread.sleep(1000)
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            val activity = activityReference.get()

            if (activity != null) {
                activity.activity_wifi_connect_waiting.text = "공유기 연결에 성공하였습니다."
                Thread.sleep(1000)

                val intent = Intent(activity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
            }
        }
    }
}