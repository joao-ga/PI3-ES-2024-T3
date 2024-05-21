package br.com.the_guardian

// importações
import android.content.Intent
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.text.InputFilter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import kotlin.math.max


class RegisterScreen : AppCompatActivity() {

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
        val cpf: String? = null,
        val birth: String? = null,
        val isManager: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_screen)

        // inicialização das variáveis do firebase e da interface do usuário
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        db = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance("southamerica-east1")

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etCpf = findViewById(R.id.etCpf)
        etNascimento = findViewById(R.id.etNascimento)
        etPassword = findViewById(R.id.etPassword)
        etPhone = findViewById(R.id.etPhone)
        tvCadastroEnviado = findViewById(R.id.tvCadastroEnviado)
        tvCadastroNegado = findViewById(R.id.tvCadastroNegado)
        btnEnviar = findViewById(R.id.btnEnviar)

        // Adiciona InputFilter para limitar o número de caracteres
        etCpf.filters = arrayOf(InputFilter.LengthFilter(11))
        etPhone.filters = arrayOf(InputFilter.LengthFilter(11))

        // Adicionar TextWatcher para formatar data de nascimento
        etNascimento.addTextChangedListener(object : TextWatcher {
            private var current = ""
            private val ddmmyyyy = "DDMMYYYY"
            private val cal = Calendar.getInstance()

            override fun beforeTextChanged(cs: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString() != current) {
                    var clean = s.toString().replace("[^\\d.]|\\.".toRegex(), "")
                    val cleanC = current.replace("[^\\d.]|\\.".toRegex(), "")

                    val cl = clean.length
                    var sel = cl
                    for (i in 2 until cl step 2) {
                        sel++
                    }

                    if (clean == cleanC) sel--

                    if (clean.length < 8) {
                        clean = clean + ddmmyyyy.substring(clean.length)
                    } else {
                        var day = Integer.parseInt(clean.substring(0, 2))
                        var mon = Integer.parseInt(clean.substring(2, 4))
                        var year = Integer.parseInt(clean.substring(4, 8))

                        if (mon > 12) mon = 12
                        cal.set(Calendar.MONTH, mon - 1)
                        year = if (year < 1900) 1900 else if (year > 2023) 2023 else year
                        cal.set(Calendar.YEAR, year)

                        day = if (day > cal.getActualMaximum(Calendar.DATE)) cal.getActualMaximum(Calendar.DATE) else day
                        clean = String.format("%02d%02d%02d", day, mon, year)
                    }

                    clean = String.format("%s/%s/%s", clean.substring(0, 2),
                        clean.substring(2, 4),
                        clean.substring(4, 8))

                    sel = if (sel < 0) 0 else sel
                    current = clean
                    etNascimento.setText(current)
                    etNascimento.setSelection(if (sel < current.count()) sel else current.count())
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        btnEnviar.setOnClickListener { view ->

            // cria variaveis para receber os dados do usuário
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val cpf = etCpf.text.toString()
            val birth = etNascimento.text.toString()
            val name = etName.text.toString()
            val phone = etPhone.text.toString()

            // valida se todos os campos estão preenchidos
            if (email.isEmpty() || password.isEmpty() || cpf.isEmpty() || birth.isEmpty() || name.isEmpty() || phone.isEmpty()) {
                // se não estiver mostra um snackbar, alertando para preencher todos os campos
                val snackbar = Snackbar.make(view, "Preencha todos os campos!", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(Color.RED)
                snackbar.show()

            } else if (isNumeric(name)) {
                etName.error = "Nome inválido"
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Email inválido"
            } else if (!isValidDate(birth)) {
                etNascimento.error = "Data de nascimento inválida"
            } else if (!isNumeric(cpf) || cpf.length != 11) {
                etCpf.error = "CPF inválido, deve conter 11 dígitos"
            } else if (!isNumeric(phone) || phone.length != 11) {
                etPhone.error = "Número de telefone inválido, deve conter 11 dígitos"
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
    private fun sendEmailVerification() {
        // user recebe o usuario atual
        val user = auth.currentUser
        Log.d("TAG", "Entrei")
        // manda o e mail de verficação para o usuário atual
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                // se der certo exibe uma mensagem de sucesso
                if (task.isSuccessful) {
                    Log.d("TAG", "Email de verificação enviado.")
                    Toast.makeText(baseContext, "Um email de verificação foi enviado para o seu endereço de email.",
                        Toast.LENGTH_SHORT).show()
                } else {
                    // se não
                    Toast.makeText(baseContext, "Email não enviado, tente de novo mais tarde",
                        Toast.LENGTH_SHORT).show()
                    Log.e("TAG", "Falha ao enviar e-mail de verificação.", task.exception)
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
                    Log.d("TAG", "createUserWithEmail:success")
                    Toast.makeText(
                        baseContext,
                        "Um email de verificação foi enviado para seu endereço de email.",
                        Toast.LENGTH_SHORT,
                    ).show()

                    // Obter o UID do usuário atualmente autenticado
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        // Chama a função addUser passando o UID do usuário
                        addUser(uid, User(uid, etName.text.toString(), email, etPhone.text.toString(), etCpf.text.toString(), etNascimento.text.toString(), false))
                    } else {
                        Log.e("TAG", "UID do usuário é nulo após a autenticação.")
                    }
                    sendEmailVerification()
                } else {
                    // Se falhar na autenticação
                    Log.w("TAG", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Email já cadastrado.",
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
            "isManager" to u.isManager
        )
        // chama a função do Firebase para cadastrar o usuário
        functions.getHttpsCallable("addUser").call(user)
            .addOnSuccessListener { result ->
                // Sucesso ao cadastrar o usuário
                val response = result.data as HashMap<*, *>
                Log.d("TAG", "Usuário cadastrado com sucesso")
            }
            .addOnFailureListener { e ->
                // Erro ao cadastrar o usuário
                Log.w("TAG", "Erro ao cadastrar usuário", e)
            }
    }

    // função que muda de tela
    private fun nextScreen(screen: Class<*>) {
        val nxScreen = Intent(this, screen)
        startActivity(nxScreen)
    }

    // Função que verifica se uma string contém apenas dígitos
    private fun isNumeric(str: String): Boolean {
        return str.all { it.isDigit() }
    }

    private fun isValidDate(date: String): Boolean {
        if (date.length != 10) return false
        val parts = date.split("/")
        if (parts.size != 3) return false
        val day = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val year = parts[2].toIntOrNull() ?: return false

        if (day !in 1..31 || month !in 1..12 || year !in 1900..2100) return false

        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        return calendar.get(Calendar.DAY_OF_MONTH) == day &&
                calendar.get(Calendar.MONTH) + 1 == month &&
                calendar.get(Calendar.YEAR) == year
    }
}
