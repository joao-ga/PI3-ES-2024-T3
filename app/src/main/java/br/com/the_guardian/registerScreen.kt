package br.com.the_guardian

// Importações
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

class registerScreen : AppCompatActivity() {

    // Variáveis de gerenciamento do Firebase, Firestore e Functions
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions

    // Variáveis de inputs, botões e texto da interface do usuário
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
    private lateinit var tvNameError: AppCompatTextView
    private lateinit var tvCpfError: AppCompatTextView
    private lateinit var tvNascimentoError: AppCompatTextView
    private lateinit var tvPhoneError: AppCompatTextView
    private lateinit var tvEmailError: AppCompatTextView
    private lateinit var tvPasswordError: AppCompatTextView

    // Classe de dados do usuário, representa os dados do usuário
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

        // Inicialização das instâncias do Firebase e da interface do usuário
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        db = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance("southamerica-east1")

        // Inicialização das views da interface do usuário
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etCpf = findViewById(R.id.etCpf)
        etNascimento = findViewById(R.id.etNascimento)
        etPassword = findViewById(R.id.etPassword)
        etPhone = findViewById(R.id.etPhone)
        tvCadastroEnviado = findViewById(R.id.tvCadastroEnviado)
        tvCadastroNegado = findViewById(R.id.tvCadastroNegado)
        btnEnviar = findViewById(R.id.btnEnviar)
        btnVoltar = findViewById(R.id.btnVoltar)
        tvNameError = findViewById(R.id.tvNameError)
        tvCpfError = findViewById(R.id.tvCpfError)
        tvNascimentoError = findViewById(R.id.tvNascimentoError)
        tvPhoneError = findViewById(R.id.tvPhoneError)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)

        // Adiciona InputFilter para limitar o número de caracteres nos campos de CPF e telefone
        etCpf.filters = arrayOf(InputFilter.LengthFilter(11))
        etPhone.filters = arrayOf(InputFilter.LengthFilter(11))

        // Adiciona TextWatcher para formatar a data de nascimento enquanto o usuário digita
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
                        clean += ddmmyyyy.substring(clean.length)
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

        // Define o comportamento do botão "Enviar"
        btnEnviar.setOnClickListener { view ->
            // Coleta os dados do usuário dos campos de entrada
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val cpf = etCpf.text.toString()
            val birth = etNascimento.text.toString()
            val name = etName.text.toString()
            val phone = etPhone.text.toString()

            // Chama a função que valida e registra o usuário
            validateAndRegisterUser(view, email, password, cpf, birth, name, phone)
        }

        // Define o comportamento do botão "Voltar"
        btnVoltar.setOnClickListener {
            // Função que muda de tela
            nextScreen(loginScreen::class.java)
        }
    }

    // Função que valida os campos e registra o usuário
    private fun validateAndRegisterUser(view: android.view.View, email: String, password: String, cpf: String, birth: String, name: String, phone: String) {
        var isError = false

        // Valida o campo nome
        if (name.isEmpty()) {
            updateInputState(etName, tvNameError, "Nome não pode estar vazio", true)
            isError = true
        } else if (isNumeric(name)) {
            updateInputState(etName, tvNameError, "Nome inválido, deve ter letras", true)
            isError = true
        } else {
            updateInputState(etName, tvNameError, "", false)
        }

        // Valida o campo email
        if (email.isEmpty()) {
            updateInputState(etEmail, tvEmailError, "Email não pode estar vazio", true)
            isError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            updateInputState(etEmail, tvEmailError, "Entre com um email válido", true)
            isError = true
        } else {
            updateInputState(etEmail, tvEmailError, "", false)
        }

        // Valida o campo data de nascimento
        if (birth.isEmpty()) {
            updateInputState(etNascimento, tvNascimentoError, "Data de nascimento não pode estar vazia", true)
            isError = true
        } else if (!isValidDate(birth)) {
            updateInputState(etNascimento, tvNascimentoError, "Insira uma data de nascimento válida", true)
            isError = true
        } else {
            updateInputState(etNascimento, tvNascimentoError, "", false)
        }

        // Valida o campo CPF
        if (cpf.isEmpty()) {
            updateInputState(etCpf, tvCpfError, "CPF não pode estar vazio", true)
            isError = true
        } else if (!isNumeric(cpf) || cpf.length != 11) {
            updateInputState(etCpf, tvCpfError, "CPF inválido, deve conter 11 dígitos", true)
            isError = true
        } else {
            updateInputState(etCpf, tvCpfError, "", false)
        }

        // Valida o campo telefone
        if (phone.isEmpty()) {
            updateInputState(etPhone, tvPhoneError, "Número de telefone não pode estar vazio", true)
            isError = true
        } else if (!isNumeric(phone) || phone.length != 11) {
            updateInputState(etPhone, tvPhoneError, "Número de telefone inválido, deve conter 11 dígitos", true)
            isError = true
        } else {
            updateInputState(etPhone, tvPhoneError, "", false)
        }

        // Valida o campo senha
        if (password.isEmpty()) {
            updateInputState(etPassword, tvPasswordError, "Senha não pode estar vazia", true)
            isError = true
        } else {
            updateInputState(etPassword, tvPasswordError, "", false)
        }

        // Exibe mensagens de erro se houver campos inválidos
        if (isError) {
            val snackbar = Snackbar.make(view, "Por favor, corrija os campos inválidos.", Snackbar.LENGTH_LONG)
            snackbar.show()
        } else {
            // Chama a função para registrar no Firebase Authenticator se não houver erros
            userRegistration(email, password)
        }
    }

    // Função que atualiza o estado visual de um campo de entrada de texto com base no status fornecido
    fun updateInputState(editText: EditText, errorTextView: TextView, tvError: String, isError: Boolean) {
        val errorIcon: Drawable? = if (isError) {
            ContextCompat.getDrawable(this, R.drawable.baseline_error_24)?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        } else {
            null
        }

        editText.setCompoundDrawables(null, null, errorIcon, null)

        if (isError) {
            editText.setBackgroundResource(R.drawable.error_edit_text_background)
            errorTextView.text = tvError
            errorTextView.visibility = View.VISIBLE
        } else {
            editText.setBackgroundResource(R.drawable.email_login)
            errorTextView.visibility = View.GONE
        }
    }

    // Função que envia e-mail de verificação para o usuário
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
                    Toast.makeText(baseContext, "Email de verificação não enviado, tente de novo mais tarde",
                        Toast.LENGTH_SHORT).show()
                    Log.e("TAG", "Falha ao enviar e-mail de verificação.", task.exception)
                }
            }
    }

    // Função que registra o usuário no Firebase Authenticator
    private fun userRegistration(email: String, password: String) {
        // cria um usuario no authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // se der certo
                if (task.isSuccessful) {
                    // mostra uma resposta pro usuário e chama a função de mandar um email de verificação
                    Log.d("TAG", "createUserWithEmail:success")
                    // Obter o UID do usuário atualmente autenticado
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        // Adiciona o usuário ao banco de dados após registro no Firebase Authenticator
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
                        "Este email já foi cadastrado.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    // Função que adiciona o usuário no banco de dados
    private fun addUser(uid: String, u: User) {
        // cria um hash pra adicionar usuario
        val user = hashMapOf(
            "uid" to uid,
            "name" to u.name,
            "email" to u.email,
            "cpf" to u.cpf,
            "birth" to u.birth,
            "phone" to u.phone,
            "isManager" to u.isManager
        )
        // chama a function que adiciona o usuario no backend
        functions.getHttpsCallable("addUser").call(user)
            .addOnSuccessListener { result ->
                // Sucesso ao cadastrar o usuário
                val response = result.data as HashMap<*, *>
                Log.d("TAG", "Usuário cadastrado com sucesso")
            }
            .addOnFailureListener { e ->
                // Erro ao cadastrar usuário
                Log.w("TAG", "Erro ao cadastrar usuário", e)
            }
    }

    // Função que muda de tela
    private fun nextScreen(screen: Class<*>) {
        val nxScreen = Intent(this, screen)
        startActivity(nxScreen)
    }

    // Função que verifica se uma string contém apenas dígitos
    private fun isNumeric(str: String): Boolean {
        return str.all { it.isDigit() }
    }

    // Função que valida se uma data está no formato DD/MM/YYYY
    private fun isValidDate(date: String): Boolean {
        if (date.length != 10) return false
        val parts = date.split("/")
        if (parts.size != 3) return false
        val day = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val year = parts[2].toIntOrNull() ?: return false

        // verificação do dia, mes e ano
        if (day !in 1..31 || month !in 1..12 || year !in 1900..2100) return false

        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        return calendar.get(Calendar.DAY_OF_MONTH) == day &&
                calendar.get(Calendar.MONTH) + 1 == month &&
                calendar.get(Calendar.YEAR) == year
    }
}
