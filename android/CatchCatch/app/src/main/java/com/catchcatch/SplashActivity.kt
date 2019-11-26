package com.catchcatch

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nhn.android.naverlogin.OAuthLogin
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var serverIP: String
    private lateinit var savedId: String
    private lateinit var loginType: String
    private lateinit var loginCheck: String

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imageView: ImageView = findViewById(R.id.activity_splash_logo)
        Glide.with(this).load(R.drawable.splah).into(imageView)

        prefs = this.getSharedPreferences("info", Context.MODE_PRIVATE)
        loginType = prefs.getString("login_type", "")!!
        loginCheck = prefs.getString("login_check", "false")!!

        if ("" == prefs.getString("server_ip", "")) {
            val editor = prefs.edit()
            editor.putString("server_ip", "15.164.75.141").apply()
        }

        // Naver Login
        val naverOAuthLoginModule = OAuthLogin.getInstance()
        naverOAuthLoginModule.init(this, getString(R.string.naver_client_id),
            getString(R.string.naver_client_secret), getString(R.string.naver_client_name))

        val naverOAuthLoginButton: OAuthLoginButton = findViewById(R.id.activity_splash_naver_login_button)
        naverOAuthLoginButton.setOAuthLoginHandler(NaverOAuthLoginHandler(this, naverOAuthLoginModule))

        // Google Login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()

        val signInIntent = googleSignInClient.signInIntent

        // Delay
        Handler().postDelayed({
            // 자동 로그인
            if (loginCheck == "true") {
                when (loginType) {
                    "naver" -> {
                        activity_splash_naver_login_button.performClick()
                    }
                    "google" -> {
                        startActivityForResult(signInIntent, RC_SIGN_IN)
                    }
                    "facebook" -> {
                        handleFacebookAccessToken(AccessToken.getCurrentAccessToken())
                    }
                }

            } else {
                val intent = Intent(this, LoginActiviy::class.java)
                startActivity(intent)
                finish()
            }
        }, 2000)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)

            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(baseContext, "Google sign in failed $e", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActiviy::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser

                    if (user?.email != savedId) {
                        editor.putString("id", user?.email).apply()
                        editor.putString("name", user?.displayName).apply()
                    }

                    val idCheckResult = AsyncTaskIdCheck().execute("http://$serverIP/id_check.php", user?.email).get()

                    if (idCheckResult == "0") {
                        AsyncTaskRegister().execute("http://$serverIP/insert.php", user?.email, "00000000", user?.displayName).get()
                    }

                    editor.putString("login_type", "google").apply()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "signInWithCredential:failure ${task.exception}", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, LoginActiviy::class.java)
                    startActivity(intent)
                    finish()
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser

                    if (user?.email != savedId) {
                        editor.putString("id", user?.email).apply()
                        editor.putString("name", user?.displayName).apply()
                    }

                    val idCheckResult = AsyncTaskIdCheck().execute("http://$serverIP/id_check.php", user?.email).get()

                    if (idCheckResult == "0") {
                        AsyncTaskRegister().execute("http://$serverIP/insert.php", user?.email, "00000000", user?.displayName).get()
                    }

                    editor.putString("login_type", "facebook").apply()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, LoginActiviy::class.java)
                    startActivity(intent)
                    finish()
                }
            }
    }
}
