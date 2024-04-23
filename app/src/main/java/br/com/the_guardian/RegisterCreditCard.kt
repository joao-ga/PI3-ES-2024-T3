package br.com.the_guardian

// impotações
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RegisterCreditCard : AppCompatActivity() {

    // variáveis para autenticação, banco de dados e funções firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions

    // variáveis para os inputs e botões
    private lateinit var etNumCartao: AppCompatEditText
    private lateinit var etName: AppCompatEditText
    private lateinit var etExpDate: AppCompatEditText
    private lateinit var etSecCode: AppCompatEditText
    private lateinit var btnEnviar: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton

    // classe para representar informações do cartão de crédito
    data class CreditCards(
        val idUser: String? = null,
        val cardNumber: String? = null,
        val cardName: String? = null,
        val expDate: String? = null,
        val secCode: String? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_credit_card)

        // inicialização das instâncias Firebase
        auth = Firebase.auth
        database = Firebase.database.reference
        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions("southamerica-east1")
        val currentUser = auth.currentUser

        // inicialização dos inputs e botões
        etNumCartao = findViewById(R.id.etNumCartao)
        etName = findViewById(R.id.etName)
        etExpDate = findViewById(R.id.etExpDate)
        etSecCode = findViewById(R.id.etSecCode)
        btnEnviar = findViewById(R.id.btnEnviar)

        getCreditCardInfos()

        btnEnviar.setOnClickListener {
            // variáveis que recebem dados dos inputs
            val cardNumber = etNumCartao.text.toString()
            val cardName = etName.text.toString()
            val expDate = etExpDate.text.toString()
            val secCode = etSecCode.text.toString()
            val idUser = currentUser?.uid

            // criação da instância card, que recebe dados da classe CreditCard
            val card = CreditCards(idUser, cardNumber, cardName, expDate, secCode)
            CoroutineScope(Dispatchers.Main).launch {
                if (idUser != null) {
                    if (idUser.isEmpty() || cardNumber.isEmpty() || cardName.isEmpty() || expDate.isEmpty() || secCode.isEmpty()) {
                        Toast.makeText(
                            baseContext,
                            "Preencha todos os campos!",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        addCreditCard(card)
                    }
                }
            }
        }

        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // botão para voltar para a home
            nextScreen(homeScreen::class.java)
        }
    }

    private fun getCreditCardInfos() {
        val firestore = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser?.uid

        if (currentUser != null) {
            firestore.collection("CreditCards")
                .whereEqualTo("idUser", currentUser)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val cardInfo = document.toObject(CreditCards::class.java)
                        if (cardInfo != null) {
                            // Preenche os campos com as informações do cartão
                            etNumCartao.setText(cardInfo.cardNumber ?: "")
                            etName.setText(cardInfo.cardName ?: "")
                            etExpDate.setText(cardInfo.expDate ?: "")
                            etSecCode.setText(cardInfo.secCode ?: "")

                            // Habilita a edição dos campos
                            etNumCartao.isEnabled = true
                            etName.isEnabled = true
                            etExpDate.isEnabled = true
                            etSecCode.isEnabled = true

                            btnEnviar.isEnabled = false
                            btnEnviar.setBackgroundResource(R.color.dark_grey)

                        } else {
                            Log.e("error", "Os dados do cartão de crédito estão vazios.")
                        }
                    } else {
                        Log.d("debugg", "Nenhum cartão de crédito encontrado para este usuário.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("DataScreen", "Erro ao recuperar dados do Firestore: $exception")
                    Toast.makeText(this, "Erro ao recuperar dados do Firestore: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("error", "Usuário atual é nulo.")
            Toast.makeText(this, "Erro: usuário não autenticado.", Toast.LENGTH_SHORT).show()
        }
    }


    // função que adiciona o cartão no banco de dados
    private fun addCreditCard(card: CreditCards) {
        // faz um map nos dados do cartão
        val cc = hashMapOf(
            "idUser" to card.idUser,
            "cardName" to card.cardName,
            "cardNumber" to card.cardNumber,
            "secCode" to card.secCode,
            "expDate" to card.expDate
        )
        // chama a função do Firebase para cadastrar o usuário
        functions.getHttpsCallable("addCreditCard").call(cc)
            .addOnSuccessListener { result ->
                // Sucesso ao cadastrar o usuário
                val response = result.data as HashMap<*, *>
                Log.d(ContentValues.TAG, "Cartão de crédito cadastrado com sucesso")
                Toast.makeText(this, "Cartão de Crédito cadastrado com sucesso.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Erro ao cadastrar o usuário
                Log.w(ContentValues.TAG, "Erro ao cadastrar Cartão de crédito", e)
                Toast.makeText(this, "Cartão não cadastrado.", Toast.LENGTH_SHORT).show()
            }
    }

    // função generica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val loginScreen = Intent(this, screen)
        startActivity(loginScreen)

    }

    private suspend fun verificarUsuarioTemCartao(): Boolean {
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
                            Log.d(ContentValues.TAG, "Cartão do usuário não encontrado")
                            continuation.resume(false)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d(
                            ContentValues.TAG,
                            "Falha ao obter se o usuário possui algum cartão:",
                            exception
                        )
                        continuation.resume(false)
                    }
            }
        }
        return hasCard
    }

}