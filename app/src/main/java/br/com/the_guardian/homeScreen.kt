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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class homeScreen : AppCompatActivity(), OnMapReadyCallback {

    private val places = arrayListOf(
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

    private lateinit var btnCadastrarCartao: AppCompatButton
    private lateinit var btnSair: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            googleMap.setInfoWindowAdapter(markerInfoAdapter(this))
            addMarkers(googleMap)

            googleMap.setOnMapLoadedCallback {
                val bounds = LatLngBounds.builder()

                places.forEach {
                    bounds.include(it.latLng)
                }

                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 150))
            }

        }

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
        mMap = googleMap
        addMarkers(mMap)
        if (places.isNotEmpty()) {
            places.forEach { place ->
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .title(place.name)
                        .snippet(place.reference)
                        .contentDescription(place.address)
                        .draggable(place.disponibility)
                        .position(place.latLng)
                )
            }
            mMap.setOnMarkerClickListener { false }

        }
    }


    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)

    }
}