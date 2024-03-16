package br.com.the_guardian

import android.annotation.SuppressLint
import android.content.ContentValues
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ktx.Firebase

class loginScreen : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth

    private lateinit var etEmailLogin: AppCompatEditText
    private lateinit var etSenhaLogin: AppCompatEditText
    private lateinit var btnEnviarLogin: AppCompatButton
    private lateinit var tvCadastrar: AppCompatTextView
    private lateinit var tvLoginNegado: AppCompatTextView
    private lateinit var tvLoginEnviado: AppCompatTextView
    private lateinit var tvEsqueceuSenha: AppCompatTextView
    private lateinit var tvStatusEsqueceuSenha: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)

        auth = Firebase.auth

        etEmailLogin = findViewById(R.id.etEmailLogin)
        etSenhaLogin = findViewById(R.id.etSenhaLogin)
        tvCadastrar = findViewById(R.id.tvCadastrar)
        tvCadastrar.setOnClickListener {
            nextScreen(registerScreen::class.java)
        }
        tvLoginNegado = findViewById(R.id.tvLoginNegado)
        tvLoginEnviado = findViewById(R.id.tvLoginEnviado)
        btnEnviarLogin = findViewById(R.id.btnEnviarLogin)

        tvEsqueceuSenha = findViewById(R.id.tvEsqueceuSenha)
        tvStatusEsqueceuSenha = findViewById(R.id.tvStatusEsqueceuSenha)

        btnEnviarLogin.setOnClickListener {view->

            val email = etEmailLogin.text.toString()
            val senha = etSenhaLogin.text.toString()

            if(email.isEmpty() || senha.isEmpty()) {
                val snackbar =
                    Snackbar.make(view, "Preencha todos os campos!", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(Color.RED)
                snackbar.show()
            } else {
                authenticator(email, senha)
            }
        }

        tvEsqueceuSenha.setOnClickListener {

            val email = etEmailLogin.text.toString()

            if(email.isEmpty()){
                tvStatusEsqueceuSenha.text = "Informe um email"
                tvStatusEsqueceuSenha.visibility = View.VISIBLE
            } else {
                val data = hashMapOf(
                    "email" to email
                )

                FirebaseFunctions.getInstance()
                    .getHttpsCallable("RecoverPassword")
                    .call(data)
                    .continueWith { task ->
                        if (!task.isSuccessful) {
                            val e = task.exception
                            if (e is FirebaseFunctionsException) {
                                val code = e.code
                                val details = e.details
                                Log.e("RecoverPassword", "Erro: $code, Detalhes: $details") // Adicionado log aqui
                            } else {

                            }
                        } else {
                            // Trate a resposta aqui
                            Log.i("RecoverPassword", "Resposta recebida: ${task.result}") // Adicionado log aqui
                        }
                    }
            }

            etEmailLogin.setOnFocusChangeListener{email, focus ->
                if(focus){
                    tvStatusEsqueceuSenha.visibility = View.GONE
                }
            }
        }
    }

    fun authenticator (etEmail: String, etSenha: String) {
        auth.signInWithEmailAndPassword(etEmail, etSenha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(ContentValues.TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    if(user?.isEmailVerified == true) {
                        nextScreen(homeScreen::class.java)
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Email não verificado",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    updateUI()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(ContentValues.TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI()

                }
            }
    }

    override fun onStart() {
        super.onStart()
        updateUI()
    }

    private fun updateUI() {
        val user: FirebaseUser? = auth.currentUser
        try{
            tvLoginEnviado.visibility = View.GONE
        } catch (e: Exception) {
            tvLoginNegado.visibility = View.GONE
        }
    }

    private fun nextScreen(screen: Class<*>) {
        val newScreem = Intent(this, screen)
        startActivity(newScreem)

    }
}