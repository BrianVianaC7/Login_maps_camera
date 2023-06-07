package com.example.login_maps_camera

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth

enum class ProvideType{
    EMAIL, GOOGLE, FACEBOOK
}


class Vprincipal : AppCompatActivity() {

    private var provider:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vprincipal)
        val bt_maps: LinearLayout = findViewById(R.id.linear1)
        bt_maps.setOnClickListener {
            val intent_a = Intent(this, VMaps::class.java)
            startActivity(intent_a)
        }
        val bt_camera: LinearLayout = findViewById(R.id.linear2)
        bt_camera.setOnClickListener {
            val intent_c = Intent(this, VCamera::class.java)
            startActivity(intent_c)
        }

    }

    private fun Salir(){
        val buider = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.activity_message, null)
        buider.setView(view)
        val dialog = buider.create()
        view.findViewById<Button>(R.id.bt_s).setOnClickListener{
            if(provider == ProvideType.FACEBOOK.name){
                LoginManager.getInstance().logOut()
            }
            FirebaseAuth.getInstance().signOut()
            val regreso: Intent = Intent(this, MainActivity::class.java)
            startActivity(regreso)
            dialog.dismiss()
            finish()
        }
        view.findViewById<Button>(R.id.bt_c).setOnClickListener{
            dialog.dismiss()
        }
        if (dialog.window != null){
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        dialog.show()
    }
}