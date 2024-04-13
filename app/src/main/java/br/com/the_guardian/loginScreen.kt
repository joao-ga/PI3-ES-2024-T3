package br.com.the_guardian

//importações
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
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ktx.Firebase
import android.content.Context
import android.content.SharedPreferences


class loginScreen : AppCompatActivity() {

    //variaveis de autenticação
    private lateinit var auth: FirebaseAuth
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    // variaveis dos inputs e botões da tela
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

        // iniciação das variáveis
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        auth = Firebase.auth

        // Inicialização dos elementos da interface do usuário

        etEmailLogin = findViewById(R.id.etEmailLogin)
        etSenhaLogin = findViewById(R.id.etSenhaLogin)
        tvCadastrar = findViewById(R.id.tvCadastrar)
        tvCadastrar.setOnClickListener {
            // botão que leva para a tela de cadastro
            nextScreen(registerScreen::class.java)
        }
        tvLoginNegado = findViewById(R.id.tvLoginNegado)
        tvLoginEnviado = findViewById(R.id.tvLoginEnviado)
        btnEnviarLogin = findViewById(R.id.btnEnviarLogin)
        tvEntrarAnonimamente = findViewById(R.id.tvEntrarAnonimamente)

        tvEsqueceuSenha = findViewById(R.id.tvEsqueceuSenha)
        tvStatusEsqueceuSenha = findViewById(R.id.tvStatusEsqueceuSenha)

        //botao de enviar o login
        btnEnviarLogin.setOnClickListener {view->

            // define o email e senha
            val email = etEmailLogin.text.toString()
            val senha = etSenhaLogin.text.toString()

            //faz verificação se email e senha foram digitados
            if(email.isEmpty() || senha.isEmpty()) {
                // snackbar passando feedback pro usuario, precisa preencher todos os campos
                val snackbar =
                    Snackbar.make(view, "Preencha todos os campos!", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(Color.RED)
                snackbar.show()
            } else {
                // se email e senha forem preenchidos chamar essa função de logar
                authenticator(email, senha)
            }
        }

        // botão de esquecer a senha
        tvEsqueceuSenha.setOnClickListener {
            // define a variavel email e chama a função de recuperar a senha
            val email = etEmailLogin.text.toString()
            recoverPassword(email)

            etEmailLogin.setOnFocusChangeListener{ email, focus ->
                if(focus){
                    tvStatusEsqueceuSenha.visibility = View.GONE
                }
            }
        }

        tvEntrarAnonimamente.setOnClickListener {
            // muda de tel sem ter feito o login para entrar anonimamente
            nextScreen(homeScreen::class.java)
        }
    }

    // funcao de recuperar senha
    private fun recoverPassword(email: String) {
        // verifica se existe um email
        if(email.isEmpty()){
           tvStatusEsqueceuSenha.text = "Digite um e-mail na caixa de texto a cima"
            tvStatusEsqueceuSenha.visibility = View.VISIBLE
        } else {
            // manda um emial de recuperacao de senha
            auth.sendPasswordResetEmail(email).addOnCompleteListener {task->
                if (!task.isSuccessful) {
                    // caso de erro
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        val code = e.code
                        val details = e.details
                        Log.e("RecoverPassword", "Erro: $code, Detalhes: $details") // Adicionado log aqui
                    }
                } else {
                    Toast.makeText(baseContext, "e-mail enviado", Toast.LENGTH_SHORT,).show()
                    Log.i("RecoverPassword", "Resposta recebida: ${task.result}") // Adicionado log aqui
                }
            }
        }
    }

    // funcao de autenticao do usuario
    private fun authenticator (etEmail: String, etSenha: String) {
        // recebe e-mail e senha para fazer a autenticacao
        auth.signInWithEmailAndPassword(etEmail, etSenha)
            .addOnCompleteListener(this) { task ->
                // se der certo
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(ContentValues.TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    // verifica se o email for verificado
                    if(user?.isEmailVerified == true) {
                        // se sim manda para tela home
                        nextScreen(homeScreen::class.java)
                    } else {
                        // se nao avisa o usuario para verificar o e mail
                        Toast.makeText(
                            baseContext,
                            "Email não verificado",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    updateUI()
                } else {
                    // Se nao, avisa o usuario que a autenticacao falhou
                    Log.w(ContentValues.TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "AAutenticação falhpou, tente de novo mais tarde",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI()

                }
        }
    }

    // verifica se o usuário ja está logado
    override fun onStart() {
        super.onStart()
        val isLoggedIn = sharedPreferences.getBoolean(getString(R.string.logged_in_key), false)
        // se ja está logado, é redirecionado para home
        if (isLoggedIn) {
            nextScreen(homeScreen::class.java)
        }
    }

    //  atualiza a interface do usuário após o login.
    private fun updateUI() {
        val user: FirebaseUser? = auth.currentUser
        if (user != null) {
            // Usuário está logado, salve o estado de login no SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putBoolean(getString(R.string.logged_in_key), true)
            editor.apply()
            // Redirecione para a tela principal
            nextScreen(homeScreen::class.java)
        }
    }

    // funçao que direciona o usuário para outra tela
    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)

    }
}