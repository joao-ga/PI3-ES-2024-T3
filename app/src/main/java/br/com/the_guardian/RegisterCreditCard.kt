package br.com.the_guardian

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
    private lateinit var btnVoltar: AppCompatButton

    data class CreditCards(
        val idUser: String? = null,
        val cardNumber: String? = null,
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
        val currentUser = auth.currentUser

        etNumCartao = findViewById(R.id.etNumCartao)
        etName = findViewById(R.id.etName)
        etExpDate = findViewById(R.id.etExpDate)
        etSecCode = findViewById(R.id.etSecCode)
        btnEnviar = findViewById(R.id.btnEnviar)
        btnEnviar.setOnClickListener {
            val cardNumber = etNumCartao.text.toString()
            val cardName = etName.text.toString()
            val expDate = etExpDate.text.toString()
            val secCode = etSecCode.text.toString()
            val idUser = currentUser?.uid

            val card = CreditCards(idUser, cardNumber, cardName, expDate, secCode)

            addCreditCard(card)
        }

        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            nextScreen(homeScreen::class.java)
        }

    }

    private fun addCreditCard(card: RegisterCreditCard.CreditCards) {
        val cc = hashMapOf(
            "idUser" to card.idUser,
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
                Toast.makeText(this, "Cartão de Crédito cadastrado com sucesso.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Erro ao cadastrar o usuário
                Log.w(ContentValues.TAG, "Erro ao cadastrar Cartão de crédito", e)
                Toast.makeText(this, "Cartão não cadastrado.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun nextScreen(screen: Class<*>) {
        val loginScreen = Intent(this, screen)
        startActivity(loginScreen)

    }
}