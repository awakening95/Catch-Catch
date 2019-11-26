package com.catchcatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import org.jetbrains.anko.wifiManager
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.activity_wifi_search.*

class WiFiSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_search)

        val availNetworks = wifiManager.scanResults
        var check = false

        for (i in 0 until (availNetworks.size - 1)) {
            if (availNetworks[i].SSID == "Catch_Catch_2019") {
                check = true
                break
            }
        }

        if (check == true) {
            Toast.makeText(this, "기기를 발견했습니다.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, WiFiSettingActivity::class.java)
            intent.putExtra("value", 0)
            startActivity(intent)
            finish()
        }
    }


    override fun onResume() {
        super.onResume()

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.setWifiEnabled(true)

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        wifiManager.startScan()
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(wifiScanReceiver)
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)

            if (success) {
                val availNetworks = wifiManager.scanResults
                var check = false

                for (i in 0 until (availNetworks.size - 1)) {
                    if (availNetworks[i].SSID == "Catch_Catch_2019") {
                        check = true
                        break
                    }
                }

                if (check == true) {
                    Toast.makeText(context, "기기를 발견했습니다.", Toast.LENGTH_SHORT).show()

                    val intent = Intent(context, WiFiSettingActivity::class.java)
                    intent.putExtra("value", 0)
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(context, "기기를 발견하지 못했습니다.\n잠시 후에 다시 시도해주세요.", Toast.LENGTH_SHORT).show()

                    val intent = Intent(context, SettingActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                }

            } else {
                Toast.makeText(context, "기기를 발견하지 못했습니다.\n잠시 후에 다시 시도해주세요.", Toast.LENGTH_SHORT).show()

                val intent = Intent(context, SettingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        }
    }
}