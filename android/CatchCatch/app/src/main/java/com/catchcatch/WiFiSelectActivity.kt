package com.catchcatch

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_wifi_select.*
import org.jetbrains.anko.wifiManager

class WiFiSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_select)

        activity_wifi_select_recycler_view.layoutManager = LinearLayoutManager(this)
        activity_wifi_select_recycler_view.setHasFixedSize(true)

        // WiFi 목록 등록
        val wifiManager = wifiManager
        val availNetworks = wifiManager.scanResults

        val wifiList = arrayListOf<String>()

        for (i in 0 until (availNetworks.size - 1)) {
            var result = true

            if(availNetworks[i].SSID != "Catch_Catch_2019" && availNetworks[i].SSID != "") {
                for (j in 0 until wifiList.size) {
                    if (availNetworks[i].SSID == wifiList[j]) {
                        result = false
                        break
                    }
                }

                if (result == true) {
                    wifiList.add(availNetworks[i].SSID)
                }
            }
        }

        activity_wifi_select_recycler_view.adapter = WiFiSelectRecyclerViewAdapter(this, wifiList)
    }
}