package com.example.login_maps_camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*

class VCamera : AppCompatActivity() {

    private var camara: Button? = null
    private var imagen: ImageView? = null
    private val CAMERA_REQUEST_CODE = 1
    private val CAMERA_PERMISSION_CODE = 1
    private val STORAGE_PERMISSION_CODE = 2

    private var provider:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vcamera)

        camara = findViewById<Button>(R.id.bt_camara)
        imagen = findViewById<ImageView>(R.id.img_foto)

        camara?.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    private fun addMarginToBitmap(bitmap: Bitmap, marginColor: String, marginSize: Int): Bitmap {
        val widthWithMargin = bitmap.width + (marginSize * 2)
        val heightWithMargin = bitmap.height + (marginSize * 2)

        val outputBitmap = Bitmap.createBitmap(widthWithMargin, heightWithMargin, bitmap.config)
        val canvas = Canvas(outputBitmap)

        val marginPaint = Paint()
        marginPaint.color = Color.parseColor(marginColor)

        // Dibujar el margen
        canvas.drawRect(
            Rect(0, 0, widthWithMargin, heightWithMargin),
            marginPaint
        )

        // Dibujar la imagen en el centro del margen
        canvas.drawBitmap(
            bitmap,
            marginSize.toFloat(),
            marginSize.toFloat(),
            null
        )

        return outputBitmap
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val extras = data?.extras
            val photo = extras?.get("data") as Bitmap

            val marginColor = "#5e2129"
            val marginSize = 3 // Tamaño del margen en píxeles
            val bitmapWithMargin = addMarginToBitmap(photo, marginColor, marginSize)

            imagen?.setImageBitmap(bitmapWithMargin)
            // Guardar la foto en Firebase Storage
            val storage = FirebaseStorage.getInstance("URL - REFERENCE")
            val storageRef = storage.reference

            // Crear un nombre único para la imagen
            val fileName = UUID.randomUUID().toString() + ".jpg"

            // Convertir la imagen en un arreglo de bytes
            val stream = ByteArrayOutputStream()
            photo.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val data = stream.toByteArray()

            // Crear una referencia al archivo en Firebase Storage
            val id: String = FirebaseAuth.getInstance().currentUser!!.uid
            val imageRef = storageRef.child("Users").child(id).child("images/$fileName")

            // Subir la imagen al almacenamiento de Firebase
            val uploadTask = imageRef.putBytes(data)
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // La imagen se cargó exitosamente a Firebase Storage
                    // Obtener la URL de descarga de la imagen
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageURL = uri.toString()
                        // Aquí puedes hacer lo que necesites con la URL de la imagen,
                        // como guardarla en la base de datos o mostrarla en la aplicación
                    }
                } else {
                    // Ocurrió un error al cargar la imagen a Firebase Storage
                    // Manejar el error según tus necesidades
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado, activalo en ajustes", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}