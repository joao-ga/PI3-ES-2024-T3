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
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions

class registerScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions


    private lateinit var etEmail: AppCompatEditText
    private lateinit var etPassword: AppCompatEditText
    private lateinit var etName: AppCompatEditText
    private lateinit var etCpf: AppCompatEditText
    private lateinit var etNascimento: AppCompatEditText
    private lateinit var etPhone: AppCompatEditText
    private lateinit var btnEnviar: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton
    private lateinit var tvCadastroEnviado: AppCompatTextView
    private lateinit var tvCadastroNegado: AppCompatTextView


    data class User(val name: String? = null,
                    val email: String? = null,
                    val phone: String? = null,
                    val cpf:String? = null,
                    val birth:String? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_screen)
        // Initialize Firebase Auth
        auth = Firebase.auth
        database = Firebase.database.reference
        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions("southamerica-east1")

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etCpf = findViewById(R.id.etCpf)
        etNascimento = findViewById(R.id.etNascimento)
        etPassword = findViewById(R.id.etPassword)
        etPhone = findViewById(R.id.etPhone)
        tvCadastroEnviado = findViewById(R.id.tvCadastroEnviado)
        tvCadastroNegado = findViewById(R.id.tvCadastroNegado)
        btnEnviar = findViewById(R.id.btnEnviar)
        btnEnviar.setOnClickListener {view->
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val cpf = etCpf.text.toString()
            val birth = etNascimento.text.toString()
            val name = etName.text.toString()
            val phone = etPhone.text.toString()

            val u = User(name, email, phone,  cpf, birth)

            if(email.isEmpty() || password.isEmpty() || cpf.isEmpty() || birth.isEmpty() || name.isEmpty()|| phone.isEmpty()) {
                val snackbar = Snackbar.make(view, "Preencha todos os campos!", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(Color.RED)
                snackbar.show()
            } else {
                userRegistration(email, password)
                addUser(u)
            }
        }

        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            nextScreen()
        }
    }


    fun sendEmailVerification(){
        val user = auth.currentUser
        Log.d(TAG, "Entrei")
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email de verificação enviado.")
                    Toast.makeText(
                        baseContext,
                        "Um e-mail de verificação foi enviado para o seu endereço de e-mail.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    Log.e(TAG, "Falha ao enviar e-mail de verificação.", task.exception)
                }
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
                    sendEmailVerification()
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


    private fun addUser(u: User) {
        val user = hashMapOf(
            "name" to u.name,
            "email" to u.email,
            "cpf" to u.cpf,
            "birth" to u.birth,
            "phone" to u.phone
        )
        Log.d(TAG, "Entrei na funcao")
        // Chame a função do Firebase para cadastrar o usuário
        functions.getHttpsCallable("addUser").call(user)
            .addOnSuccessListener { result ->
                // Sucesso ao cadastrar o usuário
                val response = result.data as HashMap<*, *>
                Log.d(TAG, "Usuário cadastrado com sucesso")
            }
            .addOnFailureListener { e ->
                // Erro ao cadastrar o usuário
                Log.w(TAG, "Erro ao cadastrar usuário", e)
            }
    }
    private fun nextScreen() {
        val nxScreen = Intent(this, loginScreen::class.java)
        startActivity(nxScreen)
    }
}