package br.com.the_guardian

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class loginScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmailLogin: AppCompatEditText
    private lateinit var etSenhaLogin: AppCompatEditText
    private lateinit var btnEnviarLogin: AppCompatButton
    private lateinit var tvCadastrar: AppCompatTextView
    private lateinit var tvLoginNegado: AppCompatTextView
    private lateinit var tvLoginEnviado: AppCompatTextView
    private lateinit var tvEsqueceuSenha: AppCompatTextView
    private lateinit var tvStatusEsqueceuSenha: AppCompatTextView
    private lateinit var tvEntrarAnonimamente: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)

        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        etEmailLogin = findViewById(R.id.etEmailLogin)
        etSenhaLogin = findViewById(R.id.etSenhaLogin)
        tvCadastrar = findViewById(R.id.tvCadastrar)
        tvCadastrar.setOnClickListener {
            nextScreen(registerScreen::class.java)
        }
        tvLoginNegado = findViewById(R.id.tvLoginNegado)
        tvLoginEnviado = findViewById(R.id.tvLoginEnviado)
        btnEnviarLogin = findViewById(R.id.btnEnviarLogin)
        tvEntrarAnonimamente = findViewById(R.id.tvEntrarAnonimamente)

        tvEsqueceuSenha = findViewById(R.id.tvEsqueceuSenha)
        tvStatusEsqueceuSenha = findViewById(R.id.tvStatusEsqueceuSenha)

        DataScreen.locacaoConfirmada = false
        getLocationInfos()

        btnEnviarLogin.setOnClickListener {view->
            val email = etEmailLogin.text.toString()
            val senha = etSenhaLogin.text.toString()
            if(email.isEmpty() || senha.isEmpty()) {
                val snackbar = Snackbar.make(view, "Preencha todos os campos!", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(Color.RED)
                snackbar.show()
            } else {
                authenticator(email, senha)
            }
        }

        tvEsqueceuSenha.setOnClickListener {
            val email = etEmailLogin.text.toString()
            recoverPassword(email)

            etEmailLogin.setOnFocusChangeListener{ email, focus ->
                if(focus){
                    tvStatusEsqueceuSenha.visibility = View.GONE
                }
            }
        }

        tvEntrarAnonimamente.setOnClickListener {
            nextScreen(homeScreen::class.java)
        }

    }

    private fun recoverPassword(email: String) {
        if(email.isEmpty()){
            tvStatusEsqueceuSenha.text = "Digite um e-mail na caixa de texto a cima"
            tvStatusEsqueceuSenha.visibility = View.VISIBLE
        } else {
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val e = task.exception
                    if (e is FirebaseAuthException) {
                        val code = e.errorCode
                        val message = e.localizedMessage
                        Log.e("RecoverPassword", "Erro: $code, Mensagem: $message")
                    }
                } else {
                    Toast.makeText(baseContext, "E-mail enviado", Toast.LENGTH_SHORT).show()
                    Log.i("RecoverPassword", "E-mail enviado com sucesso")
                }
            }
        }
    }

    private fun authenticator (etEmail: String, etSenha: String) {
        auth.signInWithEmailAndPassword(etEmail, etSenha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(ContentValues.TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    if(user?.isEmailVerified == true) {
                        verificarTipoUsuario()
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Email não verificado",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    updateUI()
                } else {
                    Log.w(ContentValues.TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Autenticação falhou, tente de novo mais tarde",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        val isLoggedIn = sharedPreferences.getBoolean(getString(R.string.logged_in_key), false)
        if (isLoggedIn) {
            verificarTipoUsuario()
        }
    }

    private fun updateUI() {
        val user: FirebaseUser? = auth.currentUser
        if (user != null) {
            val editor = sharedPreferences.edit()
            editor.putBoolean(getString(R.string.logged_in_key), true)
            editor.apply()
            nextScreen(homeScreen::class.java)
        }
    }

    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }

    private fun verificarTipoUsuario() {
        val currentUser = auth.currentUser?.uid
        db.collection("Users")
            .whereEqualTo("uid", currentUser)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val isManager = document["isManager"]
                    Log.d("is manager", isManager.toString())
                    if (isManager.toString() == "true") {
                        // Se o usuário for um gerente, direcione-o para a interface do gerente
                        nextScreen(HomeGerente::class.java)
                    } else {
                        // Se não for um gerente, direcione-o para a interface padrão
                        nextScreen(homeScreen::class.java)
                    }
                } else {
                    Log.d(ContentValues.TAG, "Documento do usuário não encontrado")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "Falha ao obter o tipo de usuário:", exception)
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
                            val locker = document["locker"]
                            val user = document["uid"]
                            val price = document["price"]
                            val time = document["startTime"]
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
                    } else {
                        Log.d(ContentValues.TAG, "Documento do usuário não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(ContentValues.TAG, "Falha ao obter o documento do usuário:", exception)
                }
        }
    }
}