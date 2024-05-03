package br.com.the_guardian

// importações
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions

class registerScreen : AppCompatActivity() {

    // variaveis de gerenciamento do firebase, firestore e functions
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions

    // variaveis de inputs, botões e texto
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


    // classe usuário, representa os dados do usuário
    data class User(
        val uid: String? = null,
        val name: String? = null,
        val email: String? = null,
        val phone: String? = null,
        val cpf:String? = null,
        val birth:String? = null,
        val hasLocker:Boolean = false,
        val isManager: Boolean = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_screen)

        // inicialização das variáveis do firebase e da interface do usuário
        auth = Firebase.auth
        database = Firebase.database.reference
        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions("southamerica-east1")
        val currentUser = auth.currentUser

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

            // cria variaveis para receber os dados do usuário
            val uid = currentUser?.uid.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val cpf = etCpf.text.toString()
            val birth = etNascimento.text.toString()
            val name = etName.text.toString()
            val phone = etPhone.text.toString()

            // valida se todos os campos estão preenchidos
            if(email.isEmpty() || password.isEmpty() || cpf.isEmpty() || birth.isEmpty() || name.isEmpty()|| phone.isEmpty()) {
                // se não estiver mostra um snackbar, alertando para preencher todos os campos
                val snackbar = Snackbar.make(view, "Preencha todos os campos!", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(Color.RED)
                snackbar.show()
            } else {
                // se estiver tudo certo ele chama função para registrar no firebase authenticator
                userRegistration(email, password)
            }
        }

        // botão para voltar pra tela de login
        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // função que muda de tela
            nextScreen(loginScreen::class.java)
        }
    }

    // função que manda e-mail de verificação para o usuário
    private fun sendEmailVerification(){
        // user recebe o usuario atual
        val user = auth.currentUser
        Log.d(TAG, "Entrei")
        // manda o e mail de verficação para o usuário atual
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                // se der certo exibe uma mensagem de sucesso
                if (task.isSuccessful) {
                    Log.d(TAG, "Email de verificação enviado.")
                    Toast.makeText(baseContext, "Um email de verificação foi enviado para o seu endereço de email.",
                        Toast.LENGTH_SHORT).show()
                } else {
                    // se não
                    Toast.makeText(baseContext, "Email não enviado, tente de novo mais tarde",
                        Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Falha ao enviar e-mail de verificação.", task.exception)

                }
            }
    }

    // função que registra usuário no firebase authenticator
    private fun userRegistration(email: String, password: String) {
        // cria um usuário no authenticator
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // se der certo
                if (task.isSuccessful) {
                    // mostra uma resposta pro usuário e chama a função de mandar um email de verificação
                    Log.d(TAG, "createUserWithEmail:success")
                    // Obter o UID do usuário atualmente autenticado
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        // Chama a função addUser passando o UID do usuário
                        addUser(uid, User(uid, etName.text.toString(), email, etPhone.text.toString(), etCpf.text.toString(), etNascimento.text.toString(), false))
                    } else {
                        Log.e(TAG, "UID do usuário é nulo após a autenticação.")
                    }
                    sendEmailVerification()
                } else {
                    // Se falhar na autenticação
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Autenticação falhou, tente novamente mais tarde.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    // função que adiciona o usuário no banco de dados
    private fun addUser(uid: String, u: User) {
        // faz um map nos dados do usuário
        val user = hashMapOf(
            "uid" to uid,
            "name" to u.name,
            "email" to u.email,
            "cpf" to u.cpf,
            "birth" to u.birth,
            "phone" to u.phone,
            "hasLocker" to u.hasLocker,
            "isManager" to u.isManager
        )
        // chama a função do Firebase para cadastrar o usuário
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
    // função que muda de tela
    private fun nextScreen(screen: Class<*>) {
        val nxScreen = Intent(this, screen)
        startActivity(nxScreen)
    }
}