package br.com.the_guardian

// importações
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class Locacao(
    val userId: String,
    val userLoc: LatLng,
    val actualLocker: homeScreen.Place?,
    val priceSelected: Double
)

class DataScreen : AppCompatActivity() {

    private var places: MutableList<homeScreen.Place> = mutableListOf(
        homeScreen.Place(
            "Armário 1",
            -22.833953,
            -47.052900,
            "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900",
            "Em frente ao prédio h15"
        ),
        homeScreen.Place(
            "Armário 2",
            -22.833877,
            -47.052470,
            "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900",
            "Em frente ao prédio h15"
        ),
        homeScreen.Place(
            "Armário 3",
            -22.834040,
            -47.051999,
            "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP",
            "Em frente ao prédio h13"
        ),
        homeScreen.Place(
            "Armário 4",
            -22.834028,
            -47.051889,
            "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP",
            "Em frente ao prédio h13"
        ),
        homeScreen.Place(
            "Armário 5",
            -22.833963,
            -47.051539,
            "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900",
            "Em frente ao prédio h11"
        ),
        homeScreen.Place(
            "Armário 6",
            -22.833928,
            -47.051418,
            "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900",
            "Em frente ao prédio h11"
        )
    )

    // variáveis para os botões
    private lateinit var btnConsultar: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton
    private lateinit var actualLocker: homeScreen.Place
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var locacaoAtual: Locacao? = null
    private lateinit var userId: String // Movendo a declaração para o escopo adequado

    private fun confirmacao(locacao: Locacao) {
        Toast.makeText(this, "Locação confirmada: $locacao", Toast.LENGTH_SHORT).show()
    }

    companion object {
        var locacoesConfirmadas = mutableListOf<Locacao>()
        // Variável global para indicar se uma locação foi confirmada, usa-la para mandar o usuario direto para tela do qrcode
        var locacaoConfirmada: Boolean = false
    }

    private var checkedRadioButtonId: Int = -1
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_screen)

        // Inicialização da FirebaseAuth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Obtendo o usuário atual
        userId = auth.currentUser?.uid ?: ""

        getLocationInfos()

        // recuperar os dados do lugar referênciado e os preços do intent
        val name = intent.getStringExtra("name")
        val reference = intent.getStringExtra("reference")
        val prices = intent.getIntArrayExtra("prices")
        val userLocLatitude = intent.getDoubleExtra("userLocLatitude", 0.0)
        val userLocLongitude = intent.getDoubleExtra("userLocLongitude", 0.0)
        val userLoc = LatLng(userLocLatitude, userLocLongitude)

        // atualizar a interface do usuário com os dados recuperados
        findViewById<TextView>(R.id.marker_title).text = "Alugar $name"
        findViewById<TextView>(R.id.marker_reference).text = reference

        // configuração dos botões de preço
        val radioButtons = listOf(
            findViewById<RadioButton>(R.id.radiobutton1),
            findViewById(R.id.radiobutton2),
            findViewById(R.id.radiobutton3),
            findViewById(R.id.radiobutton4),
            findViewById(R.id.radiobutton5)
        )

        // Recupere as strings dos recursos
        val time30Min = resources.getString(R.string.time_30_min)
        val time1h = resources.getString(R.string.time_1h)
        val time2h = resources.getString(R.string.time_2h)
        val time4h = resources.getString(R.string.time_4h)
        val time18h = resources.getString(R.string.time_18h)

        val horario = listOf(time30Min, time1h, time2h, time4h, time18h)

        // Itera sobre os preços e os configura nos botões de rádio para cada horário
        prices?.forEachIndexed { index, price ->
            if (index < radioButtons.size) {
                radioButtons[index].text = "${horario[index]} | R$ $price"
            }
        }

        // configuração da disponibilidade e cor dos botões de preço com base na disponibilidade
        radioButtons.forEach { radioButton ->
            radioButton.setTextColor( Color.rgb(160,228,24))
        }


        // verifica a hora atual para habilitar ou desabilitar o botão do dia inteiro
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        if (hour in 7..8) {
            radioButtons.last().isEnabled = true
        } else {
            radioButtons.last().isEnabled = false
            radioButtons.last().setTextColor(Color.GRAY)
        }

        if(hour >= 15){
            radioButtons[3].isEnabled = false
            radioButtons[3].setTextColor(Color.GRAY)
        }

        if(hour >= 16){
            radioButtons[2].isEnabled = false
            radioButtons[2].setTextColor(Color.GRAY)
        }

        if(hour >= 17){
            radioButtons[1].isEnabled = false
            radioButtons[1].setTextColor(Color.GRAY)
        }

        // Configurar o botão Voltar para fechar a tela
        btnVoltar = findViewById(R.id.btn_voltar)
        btnVoltar.setOnClickListener {
            finish()
        }

        // Configurar o botão Consultar para iniciar a tela de QrCode
        btnConsultar = findViewById(R.id.btn_consultar)

        btnConsultar.setOnClickListener {
            var isAnyRadioButtonChecked = false

            // verificação se algum botão de preço foi selecionado
            for (radioButton in radioButtons) {
                if (radioButton.isChecked) {
                    isAnyRadioButtonChecked = true
                    checkedRadioButtonId = radioButton.id
                    break
                }
            }

            // realiza o calculo da distancia para verificar se pode ou não realizar a locação
            fun calcularDistancia(segunda: LatLng): Double {
                val localizacaoAtual = Location("")
                localizacaoAtual.latitude = userLocLatitude
                localizacaoAtual.longitude = userLocLongitude

                val localizacaoSegunda = Location("")
                localizacaoSegunda.latitude = segunda.latitude
                localizacaoSegunda.longitude = segunda.longitude

                val distancia = localizacaoAtual.distanceTo(localizacaoSegunda) / 100.0

                return distancia
            }

            fun checkLocation(): Boolean {
                for (place in places) {
                    val locPlace = LatLng(place.latitude, place.longitude)
                    val distancia = calcularDistancia(locPlace)
                    if (distancia <= 1.0) {
                        actualLocker = place
                        return true
                    }
                }
                return false
            }

            suspend fun verificarUsuarioTemCartao(): Boolean {
                val currentUser = auth.currentUser?.uid
                var hasCard = false
                if (currentUser != null) {
                    hasCard = suspendCoroutine { continuation ->
                        db.collection("CreditCards").whereEqualTo("idUser", currentUser)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    continuation.resume(true)
                                } else {
                                    Log.d(TAG, "Cartão do usuário não encontrado")
                                    continuation.resume(false)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d(
                                    TAG,
                                    "Falha ao obter se o usuário possui algum cartão:",
                                    exception
                                )
                                continuation.resume(false)
                            }
                    }
                }
                return hasCard
            }

            fun usuarioEstaLogado(): Boolean {
                val usuarioAtual = auth.currentUser
                return usuarioAtual != null
            }

            // se algum botão foi selecionado, inicia a tela de QrCode com o preço selecionado
            if (isAnyRadioButtonChecked) {
                CoroutineScope(Dispatchers.Main).launch {
                    if (usuarioEstaLogado()) {
                        if (verificarUsuarioTemCartao()) {
                            if (checkLocation()) {
                                if (!locacaoConfirmada) {
                                    if (locacaoAtual == null) {
                                        val precoSelecionadoText =
                                            findViewById<RadioButton>(checkedRadioButtonId).text.toString()
                                        val precoNumerico =
                                            precoSelecionadoText.substringAfter("R$ ")
                                                .toDoubleOrNull()
                                        if (precoNumerico != null) {
                                            val locker = actualLocker
                                            locacaoAtual =
                                                Locacao(userId, userLoc, locker, precoNumerico)
                                            locacoesConfirmadas.add(locacaoAtual!!)
                                            confirmacao(locacaoAtual!!)
                                            addLocationInfo(name, precoSelecionadoText)
                                            val intent = Intent(baseContext, QrCodeScreen::class.java).apply {
                                                putExtra("price", precoSelecionadoText)
                                                putExtra("idArmario", name)
                                            }
                                            startActivity(intent)

                                        } else {
                                            Toast.makeText(
                                                baseContext,
                                                "Preço selecionado inválido",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            baseContext,
                                            "Você já possui uma locação pendente.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        baseContext,
                                        "Você já possui uma locação confirmada.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    baseContext,
                                    "Para realizar a locação você devera estar a no máximo 1 km de distância",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                baseContext,
                                "Para alugar um armário você precisa ter um cartão cadastrado",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Para acessar essa funcionalidade faça o login",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Escolha uma opção",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getLocationInfos() {
        val currentUser = auth.currentUser?.uid
        if (currentUser != null) {
            db.collection("Locations").whereEqualTo("uid", currentUser)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        Log.d("debugg", document.toString())
                        val isLocated = document["isLocated"]
                        Log.d("debugg", isLocated.toString())
                        if (isLocated.toString() == "true") {
                            locacaoConfirmada = true
                            val locker = document["locker"]
                            val user = document["uid"]
                            val price = document["price"]
                            val time = document["startTime"]
                            Toast.makeText(this, "Você já tem um armário pendente, apresente o QR code para o gerente!", Toast.LENGTH_LONG).show()
                            val intent = Intent(baseContext, QrCodeScreen::class.java).apply {
                                putExtra("price", price.toString())
                                putExtra("idArmario", locker.toString())
                                putExtra("user", user.toString())
                                putExtra("time", time.toString())
                            }
                            startActivity(intent)
                        }
                    } else {
                        Log.d(TAG, "Documento do usuário não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Falha ao obter o documento do usuário:", exception)
                }
        }
    }

    private fun addLocationInfo(locker: String?, price: String) {
        val currentUser = auth.currentUser?.uid
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        if (currentUser != null) {
            val locationData = hashMapOf(
                "uid" to currentUser,
                "locker" to locker,
                "price" to price,
                "startTime" to time,
                "isLocated" to true
            )
            db.collection("Locations")
                .add(locationData)
                .addOnSuccessListener { documentReference ->
                    locacaoConfirmada = true
                    Log.d("LocationInfo", "DocumentSnapshot added with ID: ${documentReference.id}")
                    Toast.makeText(this, "Locação confirmada!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.w("LocationInfo", "Error adding document", e)
                    Toast.makeText(this, "Erro, tente novamente mais tarde", Toast.LENGTH_SHORT).show()
                }
        }
    }
}