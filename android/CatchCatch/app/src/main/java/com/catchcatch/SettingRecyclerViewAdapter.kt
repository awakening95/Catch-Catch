package com.catchcatch

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.nhn.android.naverlogin.OAuthLogin
import kotlinx.android.synthetic.main.recycler_view_item_setting.view.*
import org.jetbrains.anko.backgroundColor
import java.io.File
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList


class SettingRecyclerViewAdapter(private val context: SettingActivity, private val menu: ArrayList<String>) : RecyclerView.Adapter<SettingRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.recycler_view_item_setting_text
        val imageView: ImageView = view.recycler_view_item_setting_image
        val constraintLayout: ConstraintLayout = view.recycler_view_item_setting_constraint_layout
    }

    override fun getItemCount(): Int {
        return menu.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item_setting, parent, false)

        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = menu[position]

        if (menu[position] == "WiFi 설정") {
            holder.imageView.backgroundColor = Color.rgb(0X00, 0xE6, 0xE6)
            holder.imageView.setImageResource(R.drawable.ic_wifi)

        } else if (menu[position] == "화면 조정") {
            holder.imageView.backgroundColor = Color.rgb(0XFF, 0xF7, 0x9E)
            holder.imageView.setImageResource(R.drawable.ic_resolution)

        } else if (menu[position] == "로그아웃") {
            holder.imageView.backgroundColor = Color.rgb(0XFF, 0xA3, 0x1A)
            holder.imageView.setImageResource(R.drawable.ic_logout)

        } else if (menu[position] == "시스템 초기화") {
            holder.imageView.backgroundColor = Color.rgb(0X97, 0xFF, 0x8C)
            holder.imageView.setImageResource(R.drawable.ic_reset)
        }

        holder.constraintLayout.setOnClickListener {
            if (holder.textView.text == "WiFi 설정") {
                val intent = Intent(context, WiFiSearchActivity::class.java)
                context.startActivity(intent)

            } else if (holder.textView.text == "로그아웃") {
                val prefs = context.getSharedPreferences("info", Context.MODE_PRIVATE)
                val loginType = prefs.getString("login_type", "")!!

                // Naver Logout
                if (loginType == "naver") OAuthLogin.getInstance().logout(context)

                // Google Logout
                if (loginType == "google") FirebaseAuth.getInstance().signOut()

                // Facebook Logout
                if (loginType == "facebook") LoginManager.getInstance().logOut()

                val intent = Intent(context, LoginActiviy::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                context.finish()

            } else if (holder.textView.text == "화면 조정") {
                val intent = Intent(context, CalibrationActivity::class.java)
                context.startActivity(intent)

            } else if (holder.textView.text == "시스템 초기화") {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("시스템 초기화")
                builder.setMessage("시스템을 초기화하시겠습니까?")

                builder.setPositiveButton("예") { dialog, which ->
                    val intent = Intent(context, ResetActivity::class.java)
                    context.startActivity(intent)
                }

                builder.setNegativeButton("아니오") { dialog, which -> }

                builder.show()

            }
        }
    }
}
