package br.com.the_guardian

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class homeScreen : AppCompatActivity(), OnMapReadyCallback {
    private var places: MutableList<Place> = mutableListOf(
        Place("Armário 1", LatLng(-22.833953, -47.052900), "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900", "Em frente ao prédio h15", false),
        Place("Armário 2", LatLng(-22.833877, -47.052470), "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900", "Em frente ao prédio h15", false),
        Place("Armário 3", LatLng(-22.834040, -47.051999), "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP", "Em frente ao prédio h13", false),
        Place("Armário 4", LatLng(-22.834028, -47.051889), "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP", "Em frente ao prédio h13", false),
        Place("Armário 5", LatLng(-22.833963, -47.051539), "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900", "Em frente ao prédio h11", false),
        Place("Armário 6", LatLng(-22.833928, -47.051418), "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900", "Em frente ao prédio h11", false)
        )

    data class Place(
        val name: String,
        val latLng: LatLng,
        val address: String,
        val reference: String,
        val disponibility: Boolean
    )

    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var userLoc: LatLng


    private lateinit var btnCadastrarCartao: AppCompatButton
    private lateinit var btnSair: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnCadastrarCartao = findViewById(R.id.btnCadastrarCartao)
        btnCadastrarCartao.setOnClickListener {
            if(usuarioEstaLogado()){
                nextScreen(RegisterCreditCard::class.java)
            } else {
                Toast.makeText(this, "Para acessar essa funcionalidade, você precisa fazer login.", Toast.LENGTH_SHORT).show()
            }
        }
        btnSair = findViewById(R.id.btnSair)
        btnSair.setOnClickListener {
            auth = Firebase.auth
            auth.signOut()
            nextScreen(loginScreen::class.java)
        }
    }

    private fun addMarkers(googleMap: GoogleMap) {
        places.forEach { place ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .snippet(place.reference)
                    .contentDescription(place.address)
                    .draggable(place.disponibility)
                    .position(place.latLng)
            )

            if (marker != null) {
                marker.tag = place
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        googleMap.setInfoWindowAdapter(markerInfoAdapter(this))
        mMap = googleMap
        addMarkers(mMap)
        if (checkPermission()) {
            mMap.isMyLocationEnabled = true
        } else {
            requestPermissions()
        }
    }

    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }

    private fun usuarioEstaLogado(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val usuarioAtual = auth.currentUser
        return usuarioAtual != null
    }

    // funcoes de pegar a localizacao do usuario

    private fun getCurrentLocation() {
        Log.e("debug", "entrei na getCurrentLocation")
        if(checkPermission()) {
            if(isLocationEnabled()) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) {task->
                    val location: Location?= task.result
                    if(location == null) {
                        Toast.makeText(this, "Null Recived", Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(this, "Get Success", Toast.LENGTH_SHORT).show()
                        userLoc = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 18f))
                        Log.e("debug", "AUTORIZADO")
                    }

                }


            } else {
                //setting open here
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }

        } else {

            requestPermissions()
        }

    }

    companion object {
        private var PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private fun isLocationEnabled():Boolean {
        Log.e("debug", "entrei na isLocationEnabled")


        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )

    }
    private fun requestPermissions() {
        Log.e("debug", "entrei na requestPermissions")

        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )


    }

    private fun checkPermission(): Boolean {
        Log.e("debug", "entrei na checkPermission")
        return (ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.e("debug", "entrei na onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode== PERMISSION_REQUEST_ACCESS_LOCATION) {
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()

            }
        }

    }

}