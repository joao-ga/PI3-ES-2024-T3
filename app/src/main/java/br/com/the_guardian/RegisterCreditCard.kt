package br.com.the_guardian

import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions

    private lateinit var etNumCartao: AppCompatEditText
    private lateinit var etName: AppCompatEditText
    private lateinit var etExpDate: AppCompatEditText
    private lateinit var etSecCode: AppCompatEditText
    private lateinit var btnEnviar: AppCompatButton

    data class creditCards(val cardNumber: String? = null,
                          val cardName: String? = null,
                          val expDate: String? = null,
                          val secCode: String? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_credit_card)

        auth = Firebase.auth
        database = Firebase.database.reference
        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions("southamerica-east1")

        etNumCartao = findViewById(R.id.etNumCartao)
        etName = findViewById(R.id.etName)
        etExpDate = findViewById(R.id.etExpDate)
        etSecCode = findViewById(R.id.etSecCode)
        btnEnviar = findViewById(R.id.btnEnviar)
        btnEnviar.setOnClickListener { view->
            val cardNumber = etNumCartao.text.toString()
            val cardName = etName.text.toString()
            val expDate = etExpDate.text.toString()
            val secCode = etSecCode.text.toString()

            val card = creditCards(cardNumber, cardName, expDate, secCode)

            addCreditCard(card)
        }

    }

    private fun addCreditCard(card: RegisterCreditCard.creditCards) {
        val cc = hashMapOf(
            "cardName" to card.cardName,
            "cardNumber" to card.cardNumber,
            "secCode" to card.secCode,
            "expDate" to card.expDate
        )
        Log.d(ContentValues.TAG, "Entrei na funcao")
        // Chame a função do Firebase para cadastrar o usuário
        functions.getHttpsCallable("addCreditCard").call(cc)
            .addOnSuccessListener { result ->
                // Sucesso ao cadastrar o usuário
                val response = result.data as HashMap<*, *>
                Log.d(ContentValues.TAG, "Cartão de crédito cadastrado com sucesso")
            }
            .addOnFailureListener { e ->
                // Erro ao cadastrar o usuário
                Log.w(ContentValues.TAG, "Erro ao cadastrar Cartão de crédito", e)
            }
    }
}