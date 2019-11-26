package com.catchcatch

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
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
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class LoginActiviy : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var serverIP: String
    private lateinit var savedId: String
    private lateinit var savedPassword: String
    private lateinit var loginType: String
    private lateinit var loginCheck: String

    private lateinit var id: String
    private lateinit var password: String

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = this.getSharedPreferences("info", Context.MODE_PRIVATE)
        editor = prefs.edit()

        serverIP = prefs.getString("server_ip", "")!!
        savedId =  prefs.getString("id", "")!!
        savedPassword =  prefs.getString("password", "")!!
        loginType = prefs.getString("login_type", "")!!
        loginCheck = prefs.getString("login_check", "false")!!

        // Naver Login
        val naverOAuthLoginModule = OAuthLogin.getInstance()
        naverOAuthLoginModule.init(this, getString(R.string.naver_client_id),
            getString(R.string.naver_client_secret), getString(R.string.naver_client_name))

        naver_login_button.setOAuthLoginHandler(NaverOAuthLoginHandler(this, naverOAuthLoginModule))

        // Google Login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()

        val signInIntent = googleSignInClient.signInIntent

        google_login_button.setOnClickListener {
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        // Facebook Login
        fake_facebook_login_button.setOnClickListener {
            facebook_login_button.performClick()
        }

        callbackManager = CallbackManager.Factory.create()

        facebook_login_button.setReadPermissions("email", "public_profile")
        facebook_login_button.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Log.d(TAG, "facebook:onCancel")
            }

            override fun onError(error: FacebookException) {
                Log.d(TAG, "facebook:onError", error)
            }
        })
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
                toast("Google sign in failed $e")
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
                    editor.putString("login_check", "true").apply()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)

                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    toast("signInWithCredential:failure ${task.exception}")
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
                    editor.putString("login_check", "true").apply()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)

                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}