package br.com.the_guardian

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
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
    private lateinit var tvEmailLoginError: AppCompatTextView
    private lateinit var tvESenhaLoginError: AppCompatTextView


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

        tvEmailLoginError = findViewById(R.id.tvEmailLoginError)
        tvESenhaLoginError = findViewById(R.id.tvESenhaLoginError)

        clearErrorOnTextChange(etEmailLogin, tvEmailLoginError)
        clearErrorOnTextChange(etSenhaLogin, tvESenhaLoginError)

        DataScreen.locacaoConfirmada = false
        getLocationInfos()

        btnEnviarLogin.setOnClickListener { view ->
            val email = etEmailLogin.text.toString()
            val senha = etSenhaLogin.text.toString()

            var hasError = false

            if (!isEmailValid(email)) {
                updateInputState(etEmailLogin, tvEmailLoginError, "Email inválido", true)
                hasError = true
            } else {
                updateInputState(etEmailLogin, tvEmailLoginError, "", false)
            }

            if (!isPasswordValid(senha)) {
                updateInputState(etSenhaLogin, tvESenhaLoginError, "Senha inválida", true)
                hasError = true
            } else {
                updateInputState(etSenhaLogin, tvESenhaLoginError, "", false)
            }

            if (hasError) {
                // Se houver mensagens de erro, mostra um snackbar
                val snackbar = Snackbar.make(view, "Preencha todos os campos corretamente!", Snackbar.LENGTH_LONG)
                snackbar.setBackgroundTint(Color.RED)
                snackbar.show()
            } else {
                authenticator(email, senha)
            }
        }

        tvEsqueceuSenha.setOnClickListener {
            val email = etEmailLogin.text.toString()
            recoverPassword(email)
        }

        tvEntrarAnonimamente.setOnClickListener {
            nextScreen(homeScreen::class.java)
        }
    }


    private fun isEmailValid(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotEmpty()
    }

    private fun recoverPassword(email: String) {
        if (email.isEmpty()) {
            showSnackbar(etEmailLogin, "Preencha todos os campos!", Color.RED)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            updateInputState(etEmailLogin, tvEmailLoginError, "Digite um e-mail válido", true)
        } else {
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val e = task.exception
                    val errorMessage = if (e is FirebaseAuthException) {
                        when (e.errorCode) {
                            "ERROR_INVALID_EMAIL" -> "O endereço de e-mail está mal formatado."
                            "ERROR_USER_NOT_FOUND" -> "Usuário não encontrado."
                            else -> "Falha ao enviar o e-mail de recuperação. Tente novamente mais tarde."
                        }
                    } else {
                        "Falha ao enviar o e-mail de recuperação. Tente novamente mais tarde."
                    }
                    Log.e("RecoverPassword", "Erro: ${e?.localizedMessage}")
                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                } else {
                    updateInputState(etEmailLogin, tvStatusEsqueceuSenha, "", false)
                    Toast.makeText(baseContext, "E-mail enviado com sucesso", Toast.LENGTH_SHORT).show()
                    Log.i("RecoverPassword", "E-mail enviado com sucesso")
                }
            }
        }
    }

    private fun showSnackbar(view: View, message: String, color: Int) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(color)
        snackbar.show()
    }


    private fun updateInputState(editText: AppCompatEditText, errorTextView: AppCompatTextView, errorMessage: String, isError: Boolean) {
        val errorIcon: Drawable? = if (isError) {
            ContextCompat.getDrawable(this, R.drawable.baseline_error_24)?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        } else {
            null
        }

        editText.setCompoundDrawables(null, null, errorIcon, null)
        editText.setBackgroundResource(if (isError) R.drawable.error_edit_text_background else R.drawable.email_login)

        errorTextView.text = errorMessage
        errorTextView.visibility = if (isError) View.VISIBLE else View.GONE
    }


    private fun authenticator(email: String, senha: String) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(ContentValues.TAG, "signInWithEmail:success")
                    verificarTipoUsuario()
                } else {
                    val exception = task.exception
                    val errorMessage = if (exception is FirebaseAuthException) {
                        when (exception.errorCode) {
                            "ERROR_INVALID_EMAIL" -> "O endereço de e-mail está mal formatado."
                            "ERROR_WRONG_PASSWORD" -> "Senha incorreta."
                            "ERROR_USER_NOT_FOUND" -> "Usuário não encontrado."
                            else -> "Autenticação falhou, verifique se o Email e senha estã corretos. " +
                                    " Ou tente de novo mais tarde."
                        }
                    } else {
                        "Autenticação falhou, verifique se o Email e senha estã corretos. " +
                                " Ou tente de novo mais tarde."                    }
                    if (exception is FirebaseAuthInvalidCredentialsException) {
                        val errorMessage2 = "Senha inválida."
                        updateInputState(etSenhaLogin, tvESenhaLoginError, errorMessage2, true)
                    }

                    Log.w(ContentValues.TAG, "signInWithEmail:failure", exception)
                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
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
                        val user = auth.currentUser
                        // Se não for um gerente, direcione-o para a interface padrão
                        if(user?.isEmailVerified == true) {
                            nextScreen(homeScreen::class.java)
                        } else {
                            Toast.makeText(
                                baseContext,
                                "Email não verificado",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
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

    private fun clearErrorOnTextChange(editText: AppCompatEditText, errorTextView: AppCompatTextView) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateInputState(editText, errorTextView, "", false)
            }
        })
    }

}