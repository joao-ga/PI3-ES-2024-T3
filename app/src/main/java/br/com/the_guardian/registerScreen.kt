package br.com.the_guardian

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class registerScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmail: AppCompatEditText
    private lateinit var etPassword: AppCompatEditText
    private lateinit var etName: AppCompatEditText
    private lateinit var etCpf: AppCompatEditText
    private lateinit var etNascimento: AppCompatEditText
    private lateinit var btnEnviar: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton
    private lateinit var tvCadastroEnviado: AppCompatTextView
    private lateinit var tvCadastroNegado: AppCompatTextView


    data class User(val name: String? = null,
                    val email: String? = null,
                    val password: String? = null,
                    val cpf:String? = null,
                    val birth:String? = null) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_screen)
        // Initialize Firebase Auth
        auth = Firebase.auth
        database = Firebase.database.reference
        db = FirebaseFirestore.getInstance()

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etCpf = findViewById(R.id.etCpf)
        etNascimento = findViewById(R.id.etNascimento)
        etPassword = findViewById(R.id.etPassword)
        tvCadastroEnviado = findViewById(R.id.tvCadastroEnviado)
        tvCadastroNegado = findViewById(R.id.tvCadastroNegado)
        btnEnviar = findViewById(R.id.btnEnviar)
        btnEnviar.setOnClickListener {view->
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val cpf = etCpf.text.toString()
            val birth = etNascimento.text.toString()
            val name = etName.text.toString()

            if(email.isEmpty() || password.isEmpty() || cpf.isEmpty() || birth.isEmpty() || name.isEmpty()) {
                val snackbar = Snackbar.make(view, "Preencha todos os campos!", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(Color.RED)
                snackbar.show()
            } else {
                userRegistration(email, password)
                registerUser(password, name, email, cpf, birth)
            }
        }

        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            nextScreen()
        }
    }

    private fun userRegistration(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    tvCadastroEnviado.visibility = View.VISIBLE
                    nextScreen()

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    tvCadastroNegado.visibility =View.VISIBLE

                }
            }
    }

    fun registerUser(password: String, name: String, email: String, cpf: String, birth: String?) {
        val user = User(name, email, password, cpf, birth)

        // Obtenha o ID do usuário atual
        val userId = auth.currentUser?.uid

        if (userId != null) {
            db.collection("users")
                .document(userId)  // Use o ID do usuário como identificador do documento
                .set(user)
                .addOnSuccessListener { _ ->
                    Log.d(TAG, "DocumentSnapshot adicionado com ID: $userId")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Erro ao adicionar documento", e)
                }
        } else {
            Log.w(TAG, "ID do usuário é nulo.")
        }
    }

    private fun nextScreen() {

        val nxScreen = Intent(this, loginScreen::class.java)
        startActivity(nxScreen)

    }
}