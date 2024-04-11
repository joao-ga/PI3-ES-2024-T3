package br.com.the_guardian

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import android.graphics.Color
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable

class homeScreen : AppCompatActivity(), OnMapReadyCallback, DirectionsCallback {

    private var places: MutableList<Place> = mutableListOf(
        Place("Armário 1", -22.833953, -47.052900, "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900", "Em frente ao prédio h15", false),
        Place("Armário 2", -22.833877, -47.052470, "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900", "Em frente ao prédio h15", true),
        Place("Armário 3", -22.834040, -47.051999, "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP", "Em frente ao prédio h13", false),
        Place("Armário 4", -22.834028, -47.051889, "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP", "Em frente ao prédio h13", true),
        Place("Armário 5", -22.833963, -47.051539, "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900", "Em frente ao prédio h11", false),
        Place("Armário 6", -22.833928, -47.051418, "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900", "Em frente ao prédio h11", true)
    )

    data class Place(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val reference: String,
        val disponibility: Boolean,
        var prices: List<Int> = listOf() 
     ) : Serializable


    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var userLoc: LatLng
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedMarkerLatLng: LatLng? = null
    private lateinit var btnCadastrarCartao: AppCompatButton
    private lateinit var btnSair: AppCompatButton
    private var currentPolyline: Polyline? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

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

            // Limpar o estado de login no SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putBoolean(getString(R.string.logged_in_key), false)
            editor.apply()

            // Redirecionar para a tela de login
            val intent = Intent(this, loginScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity()
        }

    }

    fun directions(destinationLatitude: Double, destinationLongitude: Double) {
        val origin = userLoc

        val geoApiContext = GeoApiContext.Builder()
            .apiKey("AIzaSyAkBu8YNk9bX1jUsK4D2hEvs8xx5wBii8w")
            .build()

        val directionsApi = DirectionsApi.newRequest(geoApiContext)
        val directionsResult = directionsApi.origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
            .destination(com.google.maps.model.LatLng(destinationLatitude, destinationLongitude))
            .await()

        val route = directionsResult.routes[0]
        val polylineOptions = PolylineOptions()
            .addAll(route.overviewPolyline.decodePath().map { convertToAndroidLatLng(it) })
            .color(Color.BLUE)
            .width(5f)

        // Remover a Polyline atual do mapa
        currentPolyline?.remove()

        // Adicionar a nova Polyline ao mapa e guardar a referência
        currentPolyline = mMap.addPolyline(polylineOptions)
    }


    private fun addMarkers(googleMap: GoogleMap) {
        places.forEach { place ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .position(LatLng(place.latitude, place.longitude))
            )

            if (marker != null) {
                marker.tag = place
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        addMarkers(mMap)
        if (checkPermission()) {
            mMap.isMyLocationEnabled = true
        } else {
            requestPermissions()
        }

        mMap.setOnMarkerClickListener { marker ->
            val place = marker.tag as? Place ?: return@setOnMarkerClickListener false

            // Criar uma instância do DialogFragment
            val dialog = pinInformation()

            // Passar as informações do marcador como argumentos
            val args = Bundle()
            args.putSerializable("place", place)
            dialog.arguments = args

            // Exibir o diálogo
            dialog.show(supportFragmentManager, "MarkerInfoDialog")

            true // Retornar true para indicar que o clique no marcador foi consumido
        }
    }

    class pinInformation : BottomSheetDialogFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.dialog_marker_info, container, false)

            val place = arguments?.getSerializable("place") as? Place

            if (place != null) {
                view.findViewById<TextView>(R.id.marker_title_home).text = place.name
                view.findViewById<TextView>(R.id.marker_reference_home).text = place.reference
                view.findViewById<TextView>(R.id.marker_address_home).text = place.address
                view.findViewById<TextView>(R.id.marker_disponibility_home).text = if (place.disponibility) "Está disponível: Sim" else "Está disponível: Não"

                val btnConsultar = view.findViewById<Button>(R.id.btnConsultar)
                btnConsultar.setOnClickListener{
                    // Obter uma referência para o contexto atual
                    val context = requireContext()

                    // Chamar a função getData para buscar os preços no Firebase
                    (context as? homeScreen)?.getData(place)

                    // Fechar o diálogo
                    dismiss()
                }

                val bntRota = view.findViewById<Button>(R.id.btnRota)
                bntRota.setOnClickListener{
                    val homeScreenActivity = activity as homeScreen
                    homeScreenActivity.selectedMarkerLatLng = LatLng(place.latitude, place.longitude)
                    homeScreenActivity.directions(place.latitude, place.longitude)
                }

            }

            return view
        }
    }

    //funcao para buscar os precos do banco de dados
    private fun getData(clickedPlace: Place) {
        val firestore = FirebaseFirestore.getInstance()
        val name = clickedPlace.name

        firestore.collection("Lockers")
            .whereEqualTo("id", name)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val prices = document["prices"] as? List<Long>
                    if (prices != null) {
                        clickedPlace.prices = prices.map { it.toInt() } // Convertendo de Long para Int
                        openDetailsScreen(clickedPlace)
                        Log.i("SUCESSO", "DADOS COLETADOS $prices")
                    } else {
                        Toast.makeText(this, "Este armário não possui preços disponíveis.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Dados não encontrados para este armário.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DataScreen", "Erro ao recuperar dados do Firestore: $exception")
                Toast.makeText(this, "Erro ao recuperar dados do Firestore.", Toast.LENGTH_SHORT).show()
            }
    }

    // Função para abrir a tela de detalhes com os dados do lugar clicado e os preços recuperados
    private fun openDetailsScreen(clickedPlace: Place) {
        val intent = Intent(this, DataScreen::class.java).apply {
            putExtra("name", clickedPlace.name)
            putExtra("reference", clickedPlace.reference)
            putExtra("disponibility", clickedPlace.disponibility)
            putExtra("prices", clickedPlace.prices.toIntArray()) // Passando os preços como um array de inteiros
        }
        startActivity(intent)
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

    private fun convertToAndroidLatLng(latLng: com.google.maps.model.LatLng): LatLng {
        return LatLng(latLng.lat, latLng.lng)
    }

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

    override fun onDirectionsRequested(destination: LatLng) {
        directions(destination.latitude, destination.longitude)
    }

}

interface DirectionsCallback {
    fun onDirectionsRequested(destination: LatLng)
}