package br.com.the_guardian

// importações
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import java.io.Serializable

class homeScreen : AppCompatActivity(), OnMapReadyCallback, DirectionsCallback {

    // lista de lugares com armários
    private var places: MutableList<Place> = mutableListOf(
        Place("Armário 1", -22.833953, -47.052900, "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900", "Em frente ao prédio h15"),
        Place("Armário 2", -22.833877, -47.052470, "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900", "Em frente ao prédio h15"),
        Place("Armário 3", -22.834040, -47.051999, "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP", "Em frente ao prédio h13"),
        Place("Armário 4", -22.834028, -47.051889, "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP", "Em frente ao prédio h13"),
        Place("Armário 5", -22.833963, -47.051539, "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900", "Em frente ao prédio h11"),
        Place("Armário 6", -22.833928, -47.051418, "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900", "Em frente ao prédio h11")
    )

    // classe representando um lugar com armário
    data class Place(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val reference: String,
        var prices: List<Int> = listOf() 
     ) : Serializable

    // variáveis e propriedades da tela
    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var userLoc: LatLng = LatLng(0.0, 0.0)
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedMarkerLatLng: LatLng? = null
    private lateinit var btnCadastrarCartao: AppCompatButton
    private lateinit var btnSair: AppCompatButton
    private lateinit var btnMinhasLocacoes: AppCompatButton
    private var currentPolyline: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        // inicialização de variáveis
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // obter a localização atual do usuário
        //getCurrentLocation()

        // obter referencia do fragmento do mapa e prepara-lo para a exibição
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // botao para ver as locacoes
        btnMinhasLocacoes = findViewById(R.id.btnMinhasLocacoes)
        btnMinhasLocacoes.setOnClickListener {
            if(usuarioEstaLogado()) {
                // se estiver ele muda de tela
                getLocationInfos()
            } else {
                // se não, aparece uma mensagem pedindo para estar logado
                Toast.makeText(this, "Para acessar essa funcionalidade, você precisa fazer login.", Toast.LENGTH_SHORT).show()
            }
        }

        // botão para acessar a tela de cadastro
        btnCadastrarCartao = findViewById(R.id.btnCadastrarCartao)
        btnCadastrarCartao.setOnClickListener {
            // validacao para saber se o usuario está logado
            if(usuarioEstaLogado()){
                // se estiver ele muda de tela
                nextScreen(RegisterCreditCard::class.java)
            } else {
                // se não, aparece uma mensagem pedindo para estar logado
                Toast.makeText(this, "Para acessar essa funcionalidade, você precisa fazer login.", Toast.LENGTH_SHORT).show()
            }
        }
        // botão para sair da conta
        btnSair = findViewById(R.id.btnSair)
        btnSair.setOnClickListener {
            auth = Firebase.auth
            auth.signOut()

            // limpar o estado de login no SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putBoolean(getString(R.string.logged_in_key), false)
            editor.apply()

            // redirecionar para a tela de login
            val intent = Intent(this, loginScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity()
        }

    }

    // função para traçar rota do usuário até o armário
    fun directions(destinationLatitude: Double, destinationLongitude: Double) {
        val origin = userLoc

        // acessa a api do google
        val geoApiContext = GeoApiContext.Builder()
            .apiKey("AIzaSyAkBu8YNk9bX1jUsK4D2hEvs8xx5wBii8w")
            .build()

        // define a rota
        val directionsApi = DirectionsApi.newRequest(geoApiContext)
        val directionsResult = directionsApi.origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
            .destination(com.google.maps.model.LatLng(destinationLatitude, destinationLongitude))
            .await()

        // traça a rota visualmente
        val route = directionsResult.routes[0]
        val polylineOptions = PolylineOptions()
            .addAll(route.overviewPolyline.decodePath().map { convertToAndroidLatLng(it) })
            .color(Color.BLUE)
            .width(5f)

        // remover a rota atual do mapa
        currentPolyline?.remove()

        // adicionar a nova rota ao mapa e guardar a referência
        currentPolyline = mMap.addPolyline(polylineOptions)
    }

    // função chamada ao solicitar rotas entre dois pontos
    override fun onDirectionsRequested(destination: LatLng) {
        directions(destination.latitude, destination.longitude)
    }

    // função para adicionar marcadores no mapa
    private fun addMarkers(googleMap: GoogleMap) {
        // faz um foreach em todos os armários
        places.forEach { place ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .position(LatLng(place.latitude, place.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(125.0f))
            )
            // adiciona os marcadores
            if (marker != null) {
                marker.tag = place
            }
        }
    }

    // função chamada quando o mapa está pronto para ser utilizado
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // chama a função para adicionar os marcadores

        addMarkers(mMap)
        // verifica se o usuario permitiu acessar a localização
        if (checkPermission()) {
            // marca no mapa a sua localização
            mMap.isMyLocationEnabled = true

            getCurrentLocation()
        } else {
            // se nao chama função para pedir a sua localização
            requestPermissions()
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 18f))

        mMap.setOnMarkerClickListener { marker ->
            val place = marker.tag as? Place ?: return@setOnMarkerClickListener false

            // criar uma instância do DialogFragment
            val dialog = pinInformation()

            // passar as informações do marcador como argumentos
            val args = Bundle()
            args.putSerializable("place", place)
            dialog.arguments = args

            // exibir o diálogo
            dialog.show(supportFragmentManager, "MarkerInfoDialog")

            // retorna true para indicar que o clique no marcador foi consumido
            true
        }
    }

    // classe para exibir o dialog
    class pinInformation : BottomSheetDialogFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            // cria o dialog
            val view = inflater.inflate(R.layout.dialog_marker_info, container, false)

            // chama a classe Place
            val place = arguments?.getSerializable("place") as? Place

            if (place != null) {
                // recebe os dados de Place
                view.findViewById<TextView>(R.id.marker_title_home).text = place.name
                view.findViewById<TextView>(R.id.marker_reference_home).text = place.reference
                view.findViewById<TextView>(R.id.marker_address_home).text = place.address

                val btnConsultar = view.findViewById<Button>(R.id.btnConsultar)
                btnConsultar.setOnClickListener{
                    // obter uma referência para o contexto atual
                    val context = requireContext()

                    // chamar a função getData para buscar os preços no Firebase
                    (context as? homeScreen)?.getData(place)

                    // fecha o diálogo
                    dismiss()
                }
                // botão para traçar a rota
                val bntRota = view.findViewById<Button>(R.id.btnRota)
                bntRota.setOnClickListener{
                    // chama a função directions para traçar a rota
                    val homeScreenActivity = activity as homeScreen
                    homeScreenActivity.selectedMarkerLatLng = LatLng(place.latitude, place.longitude)
                    homeScreenActivity.directions(place.latitude, place.longitude)
                }
            }
            return view
        }
    }

    //função para buscar os preços do banco de dados
    private fun getData(clickedPlace: Place) {
        // abre a instancia firestore
        val firestore = FirebaseFirestore.getInstance()
        val name = clickedPlace.name
        // faz a consulta na coleção lockers pelo id de dos armários
        firestore.collection("Lockers")
            .whereEqualTo("id", name)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // se receber o snapshot ele atribue a varievel prices a lista de preços que vem do banco de dados
                    val document = querySnapshot.documents[0]
                    val prices = document["prices"] as? List<Long>
                    if (prices != null) {
                        clickedPlace.prices = prices.map { it.toInt() } // convertendo de Long para Int
                        // chama função que envia os dados para outra activity
                        openDetailsScreen(clickedPlace)
                    } else {
                        Log.e("error", "Este armário não possui preços disponíveis.")
                    }
                } else {
                    Log.e("error", "Dados não encontrados para este armário.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DataScreen", "Erro ao recuperar dados do Firestore: $exception")
                Toast.makeText(this, "Erro, tente de novo mais tarde.", Toast.LENGTH_SHORT).show()
            }
    }

    // função para abrir a tela de detalhes com os dados do lugar clicado e os preços recuperados
    private fun openDetailsScreen(clickedPlace: Place) {
        val intent = Intent(this, DataScreen::class.java).apply {
            // passando os dados
            putExtra("name", clickedPlace.name)
            putExtra("reference", clickedPlace.reference)
            putExtra("prices", clickedPlace.prices.toIntArray())
            putExtra("userLocLatitude", userLoc.latitude)
            putExtra("userLocLongitude", userLoc.longitude)
        }
        // inicia nova activity
        startActivity(intent)
    }

    // função genérica para navegar para outra tela
    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }

    // verifica se o usuário está logado
    private fun usuarioEstaLogado(): Boolean {
        // recebe a instancia e o usuario atual
        val auth = FirebaseAuth.getInstance()
        val usuarioAtual = auth.currentUser
        // retuorn true, se tiver logado e false se nao
        return usuarioAtual != null
    }

    // função para converter latLng do Google Maps para latLng do Android
    private fun convertToAndroidLatLng(latLng: com.google.maps.model.LatLng): LatLng {
        return LatLng(latLng.lat, latLng.lng)
    }

    // função que pega a localização atual do usuário
    private fun getCurrentLocation() {
        // verifiva se teve permissao
        if(checkPermission()) {
            // verifica se pode acessar a localização
            if(isLocationEnabled()) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) {task->
                    val location: Location?= task.result
                    if(location == null) {
                        Toast.makeText(this, "Erro em pegar a sua localizacao, tente de novo mais tarde", Toast.LENGTH_SHORT).show()
                        Log.e("debug", "Erro em busar loc")
                    } else {
                        // user loc recebe a localização do usuário
                        userLoc = LatLng(location.latitude, location.longitude)
                        // e define a camera na localizacao do usuario
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 18f))
                    }
                }
            } else {
                // abre as configurações de localização
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            // chama a função para requisitar a permissão de pegar a localização
            requestPermissions()
        }
    }

    companion object {
        private var PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    // função para verificar se a localização está habilitada no dispositivo
    private fun isLocationEnabled():Boolean {
        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )

    }

    // função para solicitar permissões de localização
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    // função para verificar se as permissões de localização foram concedidas
    private fun checkPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    // função chamada após a solicitação de permissões de localização
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // se for aceito, o app, recebe a localização do usuario
        if(requestCode== PERMISSION_REQUEST_ACCESS_LOCATION) {
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Localização permitida", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Localização negada", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // funcao que busca as informacoes da locacao
    private fun getLocationInfos() {
        val currentUser = auth.currentUser?.uid
        if (currentUser != null) {
            // busca a locacao pelo uid do usuario
            db.collection("Locations").whereEqualTo("uid", currentUser)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val isLocated = document["isLocated"]
                        if (isLocated.toString() == "true") {
                            val locker = document["locker"]
                            val user = document["uid"]
                            val price = document["price"]
                            val time = document["startTime"]
                            // em caso de sucesso aparece uma mensagem pro usuario e manda ele para a tela de qrCode
                            Toast.makeText(this, "Você já tem um armário pendente, apresente o QR code para o gerente!", Toast.LENGTH_LONG).show()
                            DataScreen.locacaoConfirmada = true
                            val intent = Intent(baseContext, QrCodeScreen::class.java).apply {
                                putExtra("checkedRadioButtonText", price.toString())
                                putExtra("idArmario", locker.toString())
                                putExtra("user", user.toString())
                                putExtra("time", time.toString())
                            }
                            startActivity(intent)

                        }
                        // logs de erro
                    } else {
                        Toast.makeText(this, "Você não tem nenhum armario pendente.", Toast.LENGTH_LONG).show()
                        Log.d(ContentValues.TAG, "Documento do usuário não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(ContentValues.TAG, "Falha ao obter o documento do usuário:", exception)
                }
        }
    }
}

// interface para lidar com a solicitação de direções
interface DirectionsCallback {
    fun onDirectionsRequested(destination: LatLng)
}