package com.catchcatch

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WiFiSelectRecyclerViewAdapter(private val context: WiFiSelectActivity, private val wifiList: ArrayList<String>) : RecyclerView.Adapter<WiFiSelectRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.recycler_view_item_wifi_select_ssid)
        val button: Button = view.findViewById(R.id.recycler_view_item_wifi_select_check)
    }

    override fun getItemCount(): Int {
        return wifiList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item_wifi_select, parent, false)

        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = wifiList[position]

        holder.button.setOnClickListener {
            val intent = Intent(context, WiFiSettingActivity::class.java)
            intent.putExtra("value", 1)
            intent.putExtra("selected_wifi", wifiList[position])
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
            context.finish()
        }
    }
}
