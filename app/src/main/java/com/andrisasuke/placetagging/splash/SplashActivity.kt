package com.andrisasuke.placetagging.splash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.andrisasuke.placetagging.BaseActivity
import com.andrisasuke.placetagging.BuildConfig
import com.andrisasuke.placetagging.R
import com.andrisasuke.placetagging.home.HomeActivity
import com.andrisasuke.placetagging.model.UserModel
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.android.synthetic.main.splash.*

class SplashActivity: BaseActivity(), GoogleApiClient.OnConnectionFailedListener {

    companion object {
        val TAG = "SplashActivity"
    }

    private val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val RC_SIGN_IN = 9001
    private lateinit var googleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG).build()
        firebaseRemoteConfig.setConfigSettings(configSettings)
        firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults)

        //GOOGLE LOGIN
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()

        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        renderView()

        if(localPreferences.getUser() != null) {
            google_plus_login.visibility = View.GONE
            gotoHome()
        }
    }

    fun renderView() {
        overlay_bg.visibility = View.GONE
        progress_login.visibility = View.GONE

        google_plus_login.setOnClickListener{
            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun showLoading() {
        overlay_bg.visibility = View.VISIBLE
        progress_login.visibility = View.VISIBLE
    }

    private fun dismissLoading() {
        overlay_bg.visibility = View.GONE
        progress_login.visibility = View.GONE
    }
    private fun handleGoogleAccessToken(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        showLoading()
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, OnCompleteListener<AuthResult> { task ->
                    if (task.isSuccessful) {

                        val userModel = UserModel(id = task.result.user.uid,
                                        email = task.result.user.email!!,
                                        fullname = task.result.user.displayName!!,
                                        photoUrl = task.result.user.photoUrl?.toString() ?: "")
                        localPreferences.storeUser(userModel)
                        gotoHome()
                    } else {
                        Log.w(TAG, "Authentication google token failed, ${task.exception}")
                        showSnackbar(main_layout, "Failed to authenticate your google account")
                        dismissLoading()
                    }
                }).addOnFailureListener(OnFailureListener { e ->
                    Log.w(TAG, "Authentication google token failed, ${e.message}")
                    showSnackbar(main_layout, "Failed to authenticate your google account")
                    dismissLoading()
                })
    }

    fun gotoHome(){
        postDelayed(Runnable {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.e(TAG, "google client error connection ${p0.errorMessage}")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val account = result.signInAccount
                if(account != null) {
                    handleGoogleAccessToken(account)
                } else {
                    Log.e(TAG, "cant found google account")
                    showSnackbar(main_layout, "Failed to authenticate your google account")
                }
            }
        }
    }
}