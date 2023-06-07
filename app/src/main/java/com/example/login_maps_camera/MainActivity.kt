package com.example.login_maps_camera

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.ScriptGroup.Binding
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.login_maps_camera.databinding.ActivityMainBinding
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val GOOGLE_SIGN_IN = 100
    private val callbackManager = CallbackManager.Factory.create()

    private var user = ""
    private var email = ""
    private var password = ""
    private var repassword = ""

    var mDatabase: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance("url-reference").reference

        binding.btCrear.setOnClickListener {
            user = binding.btUsuario.text.toString()
            email = binding.btEmail.text.toString()
            password = binding.btPassword.text.toString()
            repassword = binding.btRepassword.text.toString()

            if(user.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && repassword.isNotEmpty()){
                if(password ==repassword){
                    if (password.length >=6){
                        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                            if (it.isSuccessful){
                                val id: String = FirebaseAuth.getInstance().currentUser!!.uid
                                val map = hashMapOf(
                                    "name" to user,
                                    "email" to email,
                                    "password" to password,
                                    "provider" to ProvideType.EMAIL
                                )
                                mDatabase!!.child("Users").child(id).setValue(map).addOnCompleteListener {
                                    if (it.isSuccessful){
                                        val intent = Intent(this, VlogIn::class.java)
                                        startActivity(intent)
                                        Toast.makeText(this, "Usuario registrado", Toast.LENGTH_SHORT).show()
                                    }else{
                                        Toast.makeText(this, "No se pudieron crear los datos correctamente", Toast.LENGTH_SHORT).show()

                                    }
                                }
                            } else {
                                Toast.makeText(this,it.exception.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else{
                        Toast.makeText(this, "La contraseña debe de tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "La contraseña no coincide", Toast.LENGTH_SHORT).show()
                }
            }else {
                Toast.makeText(this,"Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btGoogle.setOnClickListener {
            val googleConf: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).
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
                                                        val intent = Intent(this@MainActivity, Vprincipal::class.java)
                                                        startActivity(intent)
                                                        Toast.makeText(this@MainActivity, "Usuario registrado", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(this@MainActivity, "No se pudieron crear los datos correctamente", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                val intent = Intent(this@MainActivity, Vprincipal::class.java)
                                                startActivity(intent)
                                                Toast.makeText(this@MainActivity, "BIENVENIDO", Toast.LENGTH_SHORT).show()
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

        binding.txtIniciar.setOnClickListener{
            val LoginIntent = Intent(this,VlogIn::class.java)
            startActivity(LoginIntent)

        }

    }


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
                                                    val intent = Intent(this@MainActivity, Vprincipal::class.java)
                                                    startActivity(intent)
                                                    Toast.makeText(this@MainActivity, "Usuario registrado", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(this@MainActivity, "No se pudieron crear los datos correctamente", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            val intent = Intent(this@MainActivity, Vprincipal::class.java)
                                            startActivity(intent)
                                            Toast.makeText(this@MainActivity, "BIENVENIDO", Toast.LENGTH_SHORT).show()
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
}