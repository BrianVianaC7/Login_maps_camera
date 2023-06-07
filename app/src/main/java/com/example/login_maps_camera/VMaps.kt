package com.example.login_maps_camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VMaps : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private var provider:String? = null

    private lateinit var map: GoogleMap
    private lateinit var btnCalculate: Button
    private var start:String = ""
    private var end:String = ""
    var lastPattern = -1
    var poly: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null

    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vmaps)
        val addMarcador = findViewById<Button>(R.id.btnAgregarMarker)
        addMarcador.setOnClickListener {
            Toast.makeText(this, "Seleccione un punto para agregar un marcador", Toast.LENGTH_SHORT).show()
            map.setOnMapClickListener { latLng ->
                val markerOptions = MarkerOptions().position(latLng).title("Nuevo Marcador")
                map.addMarker(markerOptions)
            }
        }
        btnCalculate = findViewById(R.id.btnCalculateRoute)
        btnCalculate.setOnClickListener {
            start=""
            end=""
            poly?.remove()
            poly = null
            startMarker?.remove()
            startMarker = null
            endMarker?.remove()
            endMarker = null

            Toast.makeText(this, "Seleccione un punto de origen y destino",Toast.LENGTH_SHORT).show()
            if (::map.isInitialized){
                map.setOnMapClickListener {
                    if (start.isEmpty()){
                        //longitud, latitud
                        start="${it.longitude},${it.latitude}"
                    }else if (end.isEmpty()){
                        end ="${it.longitude},${it.latitude}"
                        createRoute()
                    }
                }
            }
        }
        createMapFragment()
    }
    private fun createMapFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        enableMyLocation()
    }

    fun changePattern(polyline: Polyline){
        lastPattern = (lastPattern + 1) % 4 // Actualiza el valor de lastPattern para el siguiente patrón
        when(lastPattern) {
            0 -> polyline.pattern = null
            1 -> polyline.pattern = listOf(Dot(), Gap(10f), Dash(50f), Gap(10f))
            2 -> polyline.pattern = listOf(Dash(30f), Gap(10f))
            3 -> polyline.pattern = listOf(Dot(), Gap(20f))
        }
    }

    private fun isPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (isPermissionsGranted()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            map.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }
    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CODE_LOCATION -> if(grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                map.isMyLocationEnabled = true
            }else{
                Toast.makeText(this, "Para activar la localización ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::map.isInitialized) return
        if(!isPermissionsGranted()){
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            map.isMyLocationEnabled = false
            Toast.makeText(this, "Para activar la localización ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "Ubicación real", Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onMyLocationClick(p0: Location) {
        Toast.makeText(this, "Coordenadas: ${p0.latitude}, ${p0.longitude}", Toast.LENGTH_SHORT).show()
    }

    //CODIGO RUTAS / OPEN ROUTE SERVICE

    private fun createRoute(){
        CoroutineScope(Dispatchers.IO).launch {
            val call = getRetrofit().create(ApiService::class.java).getRoute("TU API KEY",start,end)
            if (call.isSuccessful){
                drawRoute(call.body())
            }else{
            }
        }
    }

    private fun drawRoute(routeResponse: RouteResponse?) {
        val polylineOptions = PolylineOptions()
        val coordinates = routeResponse?.features?.first()?.geometry?.coordinates
        coordinates?.forEach {
            polylineOptions.add(LatLng(it[1], it[0]))
                .color(Color.rgb(33, 150, 243))
        }

        val startPoint = coordinates?.firstOrNull()?.let { LatLng(it[1], it[0]) }
        val endPoint = coordinates?.lastOrNull()?.let { LatLng(it[1], it[0]) }

        runOnUiThread {
            val polyline = map.addPolyline(polylineOptions)
            polyline.isClickable = true
            map.setOnPolylineClickListener { polyline -> changePattern(polyline) }

            poly = polyline
            startPoint?.let { startMarker = addMarker(it, "Origen") }
            endPoint?.let { endMarker = addMarker(it, "Destino") }
        }
    }

    private fun addMarker(latLng: LatLng, title: String): Marker? {
        val markerOptions = MarkerOptions().position(latLng).title(title)
        val marker = map.addMarker(markerOptions)
        if (marker != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 18f), 2000, null)
        }
        return marker
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder().baseUrl("https://api.openrouteservice.org/").addConverterFactory(
            GsonConverterFactory.create()).build()
    }
}