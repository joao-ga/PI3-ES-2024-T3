package br.com.the_guardian

// importacoes
import android.content.ContentValues
import android.content.Intent
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuthException

class loginScreen : AppCompatActivity() {

    // declarando variaveios uteis para a Activity
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

        // inicioalizando as variaveis
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        etEmailLogin = findViewById(R.id.etEmailLogin)
        etSenhaLogin = findViewById(R.id.etSenhaLogin)
        tvCadastrar = findViewById(R.id.tvCadastrar)
        tvCadastrar.setOnClickListener {
            // botao de cadastrar leva o usuario para a tela de cadastro
            nextScreen(registerScreen::class.java)
        }
        tvLoginNegado = findViewById(R.id.tvLoginNegado)
        tvLoginEnviado = findViewById(R.id.tvLoginEnviado)
        btnEnviarLogin = findViewById(R.id.btnEnviarLogin)
        tvEntrarAnonimamente = findViewById(R.id.tvEntrarAnonimamente)

        tvEsqueceuSenha = findViewById(R.id.tvEsqueceuSenha)
        tvStatusEsqueceuSenha = findViewById(R.id.tvStatusEsqueceuSenha)

        // boatao para fazee o login
        btnEnviarLogin.setOnClickListener {view->
            // faz as verificacoes se recebeu o email e senha, caso de sucesso chamou a funcao authenticator
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

        // botao de esqueceu a senha, chama a funcao que restaura a senha do usuario
        tvEsqueceuSenha.setOnClickListener {
            val email = etEmailLogin.text.toString()
            recoverPassword(email)

            etEmailLogin.setOnFocusChangeListener{ email, focus ->
                if(focus){
                    tvStatusEsqueceuSenha.visibility = View.GONE
                }
            }
        }

        // text view para o usuario entrar anonimamente
        tvEntrarAnonimamente.setOnClickListener {
            nextScreen(homeScreen::class.java)
        }

        // verifica se o usuario tem um armario locado
        verificarLocacaoUsuario()
    }

    // funcao que manda um email; para o usuario mudar de senha
    private fun recoverPassword(email: String) {
        // verifica se recebeu o email
        if(email.isEmpty()){
            tvStatusEsqueceuSenha.text = "Digite um e-mail na caixa de texto a cima"
            tvStatusEsqueceuSenha.visibility = View.VISIBLE
        } else {
            // manda email
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // se deu erro avisa o usuario
                    val e = task.exception
                    if (e is FirebaseAuthException) {
                        val code = (e as FirebaseAuthException).errorCode
                        val message = e.localizedMessage
                        Log.e("RecoverPassword", "Erro: $code, Mensagem: $message")
                    }
                } else {
                    // caso de sucesso manda mensagem de sucesso
                    Toast.makeText(baseContext, "E-mail enviado", Toast.LENGTH_SHORT).show()
                    Log.i("RecoverPassword", "E-mail enviado com sucesso")
                }
            }
        }
    }


    // fuyncao de autehnticar o usuario
    private fun authenticator (etEmail: String, etSenha: String) {
        // chama a funcao singInWithEmailAndPassword
        auth.signInWithEmailAndPassword(etEmail, etSenha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // caso de sucesso, user recebe o auth.currentUser
                    Log.d(ContentValues.TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    // se o usuario verificar o email ee manda para outra tela
                    if(user?.isEmailVerified == true) {
                        nextScreen(homeScreen::class.java)
                    } else {
                        // caso nao, ele recebe um aviso de verificar o email
                        Toast.makeText(
                            baseContext,
                            "Email não verificado",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    updateUI()
                } else {
                    // se a autenticacao ele avisa o usuario
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

    // funcao on start para quando a activity comecar ele verifica se o usuario ja estava logado se sim ele muda de tel
    override fun onStart() {
        super.onStart()
        val isLoggedIn = sharedPreferences.getBoolean(getString(R.string.logged_in_key), false)
        if (isLoggedIn) {
            nextScreen(homeScreen::class.java)
        }
    }

    private fun updateUI() {
        // obtém o usuário atualmente autenticado
        val user: FirebaseUser? = auth.currentUser
        // verifica se há um usuário autenticado
        if (user != null) {
            // se o usuário estiver autenticado, atualiza o estado de login no SharedPreferences
            val editor = sharedPreferences.edit()
            // Define o valor true para indicar que o usuário está logado
            editor.putBoolean(getString(R.string.logged_in_key), true)
            editor.apply()
            // navega para a próxima tela apos o login bem sucedido
            nextScreen(homeScreen::class.java)
        }
    }

    // funcao generica que muda de tela
    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }

    private fun verificarLocacaoUsuario() {
        // obtém o id do usuário
        val currentUser = auth.currentUser?.uid
        // Verifica se há um usuário autenticado
        if (currentUser != null) {
            // consulta o banco de dados para encontrar o documento do usuário com o uid correspondente
            db.collection("Users").whereEqualTo("uid", currentUser)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    // verifica se o resultado da consulta não está vazio
                    if (!querySnapshot.isEmpty) {
                        // obtém o primeiro documento retornado pela consulta
                        val document = querySnapshot.documents[0]
                        // Obtém o valor da chave "hasLocker" do documento
                        val hasLocker = document["hasLocker"]
                        Log.d("debugg", hasLocker.toString())
                        // Verifica se o usuário já possui um armário
                        if (hasLocker.toString() == "true") {
                            // Define a locação como confirmada na tela de dados
                            DataScreen.locacaoConfirmada = true
                            // Se o usuário tiver um armário, exibe uma mensagem informando e direciona para a tela do QR code
                            Toast.makeText(this, "Você já tem um armário pendente, apresente o QR code para o gerente!", Toast.LENGTH_LONG).show()
                            enviarParaTelaQRCode()
                        }
                    } else {
                        // Se o documento do usuário não for encontrado, registra uma mensagem de aviso
                        Log.d(ContentValues.TAG, "Documento do usuário não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    // Registra uma mensagem de erro em caso de falha na obtenção do documento do usuário
                    Log.d(ContentValues.TAG, "Falha ao obter o documento do usuário:", exception)
                }
        }
    }

// funfao que envia o usuario para a tela de qrcode
    private fun enviarParaTelaQRCode() {
        val intent = Intent(this, QrCodeScreen::class.java).apply {
        }
        startActivity(intent)
    }
}
