package com.example.login_maps_camera

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.login_maps_camera.databinding.ActivityVlogInBinding
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

class VlogIn : AppCompatActivity() {

    private lateinit var binding: ActivityVlogInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val GOOGLE_SIGN_IN = 100
    private val callbackManager = CallbackManager.Factory.create()

    private var user = ""
    private var email = ""
    private var password = ""

    var mDatabase: DatabaseReference? = null

    private var holdMessage: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vlog_in)
        mDatabase = FirebaseDatabase.getInstance("URL-REFERENCE").reference

        binding = ActivityVlogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val btlogin = findViewById<Button>(R.id.btlogin)
        val email_tx = findViewById<EditText>(R.id.bt_email)
        val password_tx = findViewById<EditText>(R.id.bt_pass)

        btlogin.setOnClickListener{

            email = email_tx.text.toString()
            password = password_tx.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()){

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener{
                    if (it.isSuccessful){
                        val intent = Intent(this, Vprincipal::class.java)
                        startActivity(intent)
                        finish()
                    }else {
                        showMessage()
                    }
                }
            } else {
                Toast.makeText(this, "Contraseña Incorrecta", Toast.LENGTH_SHORT).show()
            }
        }

        binding.txtPass.setOnClickListener{
            val buider = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.activity_recover, null)
            val userEmail = view.findViewById<EditText>(R.id.editBox)

            buider.setView(view)
            val dialog = buider.create()

            view.findViewById<Button>(R.id.btR).setOnClickListener{
                holdMessage = ProgressDialog(this)
                holdMessage?.setMessage("Espera un momento...")
                holdMessage?.setCancelable(false)
                holdMessage?.show()
                recoverpass(userEmail)
                dialog.dismiss()
            }
            view.findViewById<Button>(R.id.btC).setOnClickListener{
                dialog.dismiss()
            }
            if (dialog.window != null){
                dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            dialog.show()
        }
        binding.txtSingup.setOnClickListener{
            val signupIntent = Intent(this,MainActivity::class.java)
            startActivity(signupIntent)
        }

        binding.btGoogle.setOnClickListener {
            val googleConf:GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).
            requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

            val googleClient: GoogleSignInClient = GoogleSignIn.getClient(this,googleConf)
            googleClient.signOut()
            startActivityForResult(googleClient.signInIntent,GOOGLE_SIGN_IN)

        }
        binding.btFacebook.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
            LoginManager.getInstance().registerCallback(callbackManager, object :
                FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    result?.let {
                        val token: AccessToken = it.accessToken
                        val credential = FacebookAuthProvider.getCredential(token.token)
                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { signInTask ->
                            if (signInTask.isSuccessful) {
                                user = signInTask.result?.user?.displayName ?: ""
                                email = signInTask.result?.user?.email ?: ""

                                val firebaseAuth = FirebaseAuth.getInstance()
                                val userId = firebaseAuth.currentUser?.uid

                                if (userId != null) {
                                    val userRef = mDatabase!!.child("Users").child(userId)
                                    userRef.addListenerForSingleValueEvent(object :
                                        ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (!snapshot.exists()) {
                                                val map = hashMapOf(
                                                    "name" to user,
                                                    "email" to email,
                                                    "password" to password,
                                                    "provider" to ProvideType.FACEBOOK
                                                )

                                                userRef.setValue(map).addOnCompleteListener { createUserTask ->
                                                    if (createUserTask.isSuccessful) {
                                                        val intent = Intent(this@VlogIn, Vprincipal::class.java)
                                                        startActivity(intent)
                                                        Toast.makeText(this@VlogIn, "Usuario registrado", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(this@VlogIn, "No se pudieron crear los datos correctamente", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                val intent = Intent(this@VlogIn, Vprincipal::class.java)
                                                startActivity(intent)
                                                Toast.makeText(this@VlogIn, "BIENVENIDO", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Manejar el error de base de datos aquí si es necesario
                                        }
                                    })
                                }
                            } else {
                                showMessage()
                            }
                        }
                    }
                }

                override fun onCancel() {

                }

                override fun onError(error: FacebookException) {
                    showMessage()
                }
            })
        }
    }

    //Afuera

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            user = account.displayName ?: ""
                            email = account.email ?: ""

                            val firebaseAuth = FirebaseAuth.getInstance()
                            val userId = firebaseAuth.currentUser?.uid

                            if (userId != null) {
                                val userRef = mDatabase!!.child("Users").child(userId)
                                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (!snapshot.exists()) {
                                            val map = hashMapOf(
                                                "name" to user,
                                                "email" to email,
                                                "password" to password,
                                                "provider" to ProvideType.GOOGLE
                                            )

                                            userRef.setValue(map).addOnCompleteListener { createUserTask ->
                                                if (createUserTask.isSuccessful) {
                                                    val intent = Intent(this@VlogIn, Vprincipal::class.java)
                                                    startActivity(intent)
                                                    Toast.makeText(this@VlogIn, "Usuario registrado", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(this@VlogIn, "No se pudieron crear los datos correctamente", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            val intent = Intent(this@VlogIn, Vprincipal::class.java)
                                            startActivity(intent)
                                            Toast.makeText(this@VlogIn, "BIENVENIDO", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // Manejar el error de base de datos aquí si es necesario
                                    }
                                })
                            }
                        } else {
                            showMessage()
                        }
                    }
                }
            } catch (e: ApiException) {
                showMessage()
            }
        }
    }

    private fun showMessage(){
        val builder= AlertDialog.Builder(this)
        builder.setTitle("ERROR")
        builder.setMessage("Se ha producido un error al iniciar sesión")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun recoverpass(email: EditText){
        if (email.text.toString().isEmpty()){
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()){
            return
        }

        firebaseAuth.sendPasswordResetEmail(email.text.toString()).addOnCompleteListener{ task ->
            if (task.isSuccessful){
                Toast.makeText(this, "Revisa tu correo electronico", Toast.LENGTH_SHORT).show()
            } else{
                Toast.makeText(this, "Correo electronico invalido", Toast.LENGTH_SHORT).show()
            }
            holdMessage?.dismiss()
        }
    }
}