package com.onnetsolution.login

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_auth.*


class AuthActivity : AppCompatActivity() {

    private val LENGTH_PASSWORD = 6
    private val GOOGLE_SIGN_IN = 100

    private val callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        //Analytics Firebase
        val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle =  Bundle()
        bundle.putString("message", "Integración de firebase completado")
        analytics.logEvent("InitScreen", bundle)

        //Setup
        setup()
        session()

    }

    private fun session(){

        val prefs: SharedPreferences = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if(email != null && provider != null){
            showHome(email, ProviderType.valueOf(provider))
        }

    }

    private fun setup(){

        title = "Autenticación"

        signUpButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()){
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString())
                    .addOnCompleteListener {
                        if (it.isSuccessful){
                            showHome(it.result?.user?.email?: "", ProviderType.BASIC)
                        } else{
                            showAlert()
                        }
                    }
            }
        }

        logInButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.length >= LENGTH_PASSWORD){
                FirebaseAuth.getInstance().signInWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString())
                    .addOnCompleteListener {
                        if (it.isSuccessful){
                            showHome(it.result?.user?.email?: "", ProviderType.BASIC)
                        } else{
                            Log.e("Error Login", it.exception.toString())
                            showAlert()
                        }
                    }
                FirebaseAuth.getInstance().currentUser
            }
        }

        googleButton.setOnClickListener {

            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)

            googleClient.signOut()

            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }

        facebookButton.setOnClickListener {

            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile",
                "user_birthday", "user_age_range", "user_location", "user_link"))

            LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {

                override fun onSuccess(result: LoginResult?) {

                    result?.let {

                        val token = it.accessToken

                        val credential = FacebookAuthProvider.getCredential(token.token)

                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {

                            if (it.isSuccessful){
                                showHome(it.result?.user?.email?:"", ProviderType.FACEBOOK)

                                //Profile Facebook
                                val profile: Profile = Profile.getCurrentProfile()

                                val s = it.result?.additionalUserInfo?.profile
                                Log.e("data", "$s")

                                for(d in token.permissions.iterator())
                                    Log.e("Token", d)

                            } else{
                                showAlert()
                            }

                        }

                    }

                }

                override fun onCancel() {
                    Toast.makeText(applicationContext, "Cancel", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException?) {
                    showAlert()
                }

            }

            )
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        callbackManager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)

                if(account != null){

                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {

                        if (it.isSuccessful){
                            showHome(account.email?:"", ProviderType.GOOGLE)

                            val s = account.account

                            Log.e("data google", "$s")

                        } else{
                            showAlert()
                        }

                    }

                }
            } catch (e: ApiException){
                showAlert()
            }

        }

    }

    private fun showAlert(){

        val builder = AlertDialog.Builder(this)

        builder.setTitle("Alert")
        builder.setMessage("Se ha producido un error autenticando al usuario.")
        builder.setPositiveButton("Aceptar", null)
        builder.create().show()

    }

    private fun showHome(email: String, provider: ProviderType){

        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }

        startActivity(homeIntent)
    }

}