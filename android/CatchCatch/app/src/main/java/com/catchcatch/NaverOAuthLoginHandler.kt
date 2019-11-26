package com.catchcatch

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.nhn.android.naverlogin.OAuthLogin
import com.nhn.android.naverlogin.OAuthLoginHandler
import org.json.JSONObject

class NaverOAuthLoginHandler(private val mContext: Context, private val naverOAuthLoginModule: OAuthLogin) : OAuthLoginHandler() {

    private val prefs = mContext.getSharedPreferences("info", Context.MODE_PRIVATE)!!
    private val editor = prefs.edit()
    private val serverIP = prefs.getString("server_ip", "")!!
    private val id = prefs.getString("id", "")!!

    private val tag = "NaverOAuthLoginHandler"

    override fun run(success: Boolean) {
        if (success) {
            val accessToken = naverOAuthLoginModule.getAccessToken(mContext)

            val getNaverUserInfoeTask = AsyncTaskGetNaverUserInfo()
            val getNaverUserInfoeResult = getNaverUserInfoeTask.execute(accessToken).get()

            val jsonObject = JSONObject(getNaverUserInfoeResult)
            val resposejsonObject = jsonObject.getJSONObject("response")
            val email = resposejsonObject.getString("email")
            val name = resposejsonObject.getString("name")

            if (email != id) {
                editor.putString("id", email).apply()
                editor.putString("name", name).apply()
            }

            val idCheckResult = AsyncTaskIdCheck().execute("http://$serverIP/id_check.php", email).get()

            if (idCheckResult == "0") {
                AsyncTaskRegister().execute("http://$serverIP/insert.php", email, "00000000", name)
            }

            editor.putString("login_type", "naver").apply()
            editor.putString("login_check", "true").apply()

            val intent = Intent(mContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)

        } else {
            val errorCode = naverOAuthLoginModule.getLastErrorCode(mContext).code
            val errorDesc = naverOAuthLoginModule.getLastErrorDesc(mContext)
            Toast.makeText(mContext, "errorCode: $errorCode errorDesc: $errorDesc", Toast.LENGTH_SHORT).show()

            val intent = Intent(mContext, LoginActiviy::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        }
    }
}