package br.com.the_guardian

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class homeScreen : AppCompatActivity(), OnMapReadyCallback {

    private val places = arrayListOf(
        Place("Google", LatLng(-23.5868031,-46.6847268), "Av. Brig. Faria Lima, 3477 - 18º Andar - Itaim Bibi, São Paulo - SP, 04538-133", 4.8f),
        Place("Parque", LatLng(-23.5902467, -46.6639756), "Av. República do Líbano, 1157 - Ibirapuera, São Paulo - SP, 04502-001", 4.9f)
        )

    data class Place(
        val name: String,
        val latLng: LatLng,
        val address: String,
        val rating: Float
    )

    private lateinit var mMap: GoogleMap

    private lateinit var btnCadastrarCartao: AppCompatButton
    private lateinit var btnSair: AppCompatButton
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnCadastrarCartao = findViewById(R.id.btnCadastrarCartao)
        btnCadastrarCartao.setOnClickListener {
            nextScreen(RegisterCreditCard::class.java)
        }
        btnSair = findViewById(R.id.btnSair)
        btnSair.setOnClickListener {
            auth = Firebase.auth
            auth.signOut()
            nextScreen(loginScreen::class.java)
        }
    }

    fun addMarkers(googleMap: GoogleMap) {
        places.forEach { place ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .position(place.latLng)
            )
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        addMarkers(mMap)
        if (places.isNotEmpty()) {
            places.forEach { place ->
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .title(place.name)
                        .position(place.latLng)
                )
            }
            val firstPlace = places[0]
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPlace.latLng, 12f))
        }
    }

    private fun nextScreen(screen: Class<*>) {
        val newScreem = Intent(this, screen)
        startActivity(newScreem)

    }
}