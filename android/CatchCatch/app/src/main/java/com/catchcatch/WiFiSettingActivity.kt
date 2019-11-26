package com.catchcatch

import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import kotlinx.android.synthetic.main.activity_wifi_setting.*
import org.jetbrains.anko.wifiManager
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import java.lang.ref.WeakReference
import java.util.*

class WiFiSettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_setting)

        intent = intent
        val value = intent.extras!!.getInt("value")

        if (value == 1) {
            val selectedWifi = intent.extras!!.getString("selected_wifi")
            activity_wifi_setting_ssid.text = Editable.Factory.getInstance().newEditable(selectedWifi)
        }

        activity_wifi_setting_ssid_shadow.setOnClickListener {
            val intent = Intent(this, WiFiSelectActivity::class.java)
            startActivity(intent)
        }

        activity_wifi_setting_connect.setOnClickListener {
            wifiManager.setWifiEnabled(true)

            val ssid = activity_wifi_setting_ssid.text.toString()
            val key = activity_wifi_setting_key.text.toString()
            var capabilities: String = ""

            val networkList = wifiManager.scanResults
            for (network in networkList) {
                if (network.SSID == ssid) {
                    capabilities = network.capabilities
                }
            }

            val conf = WifiConfiguration()
            conf.SSID = "\"" + ssid + "\""
            conf.status = WifiConfiguration.Status.DISABLED
            conf.priority = 40

            if (capabilities.toUpperCase(Locale.getDefault()).contains("WEP")) {
                Log.d("WiFiSettingActivity", "WEP")
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)

                if (key.matches(Regex("^[0-9a-fA-F]+$"))) {
                    conf.wepKeys[0] = key
                } else {
                    conf.wepKeys[0] = "\"" + key + "\""
                }

                conf.wepTxKeyIndex = 0

            } else if (capabilities.toUpperCase(Locale.getDefault()).contains("WPA")) {
                Log.d("WiFiSettingActivity", "WPA")
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)

                conf.preSharedKey = "\"" + key + "\""

            } else { // OPEN
                Log.d("WiFiSettingActivity", "OPEN")
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                conf.allowedAuthAlgorithms.clear()
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            }

            val netId = wifiManager.addNetwork(conf)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()

            CheckWiFiConnection(this, ssid, key).execute()
        }
    }

    class CheckWiFiConnection(context: WiFiSettingActivity, ssid: String, key: String) : AsyncTask<String, String, Boolean>() {
        private val activityReference: WeakReference<WiFiSettingActivity> = WeakReference(context)
        private val ssid = ssid
        private val key = key

        override fun doInBackground(vararg p0: String?): Boolean {
            val activity = activityReference.get()
            var wifiConnectionSuccess = false

            for (i in 1..100) {
                if (activity != null) {
                    val info = activity.wifiManager.connectionInfo
                    val networkId = info.networkId

                    if (networkId != -1) {
                        wifiConnectionSuccess = true
                        break
                    }
                }
            }

            if (wifiConnectionSuccess == true) {
                return true
            }
            return false
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)

            val activity = activityReference.get()

            if (activity != null) {
                if (result == true) {
                    val intent = Intent(activity, WiFiConnectActivity::class.java)
                    intent.putExtra("ssid", ssid)
                    intent.putExtra("key", key)
                    activity.startActivity(intent)
                    activity.finish()
                } else {
                    Toast.makeText(activity, "로그인에 실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}