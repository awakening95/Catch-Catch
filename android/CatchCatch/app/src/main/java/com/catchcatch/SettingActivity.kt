package com.catchcatch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        activity_setting_recycler_view.layoutManager = LinearLayoutManager(this)
        activity_setting_recycler_view.setHasFixedSize(true)

        val menuList = arrayListOf<String>()
        menuList.add("WiFi 설정")
        menuList.add("화면 조정")
        menuList.add("로그아웃")
        menuList.add("시스템 초기화")

        activity_setting_recycler_view.adapter = SettingRecyclerViewAdapter(this, menuList)
    }
}
