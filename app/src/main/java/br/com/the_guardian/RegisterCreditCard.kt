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
        btnEnviar.setOnClickListener {
            // variáveis que recebem dados dos inputs
            val cardNumber = etNumCartao.text.toString()
            val cardName = etName.text.toString()
            val expDate = etExpDate.text.toString()
            val secCode = etSecCode.text.toString()
            val idUser = currentUser?.uid

            // criação da instância card, que recebe dados da classe CreditCard
            val card = CreditCards(idUser, cardNumber, cardName, expDate, secCode)

            if (idUser != null) {
                if(idUser.isEmpty() || cardNumber.isEmpty() || cardName.isEmpty() || expDate.isEmpty() || secCode.isEmpty()) {
                    Toast.makeText(
                        baseContext,
                        "Preencha todos os campos!",
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    Toast.makeText(
                        baseContext,
                        "Cartão de Crédito inserido com sucesso",
                        Toast.LENGTH_SHORT,
                    ).show()
                    addCreditCard(card)
                }
            }
        }

        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // botão para voltar para a home
            nextScreen(homeScreen::class.java)
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
}