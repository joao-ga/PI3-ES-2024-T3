package br.com.the_guardian

// impotações
import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.Drawable
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterCreditCard : AppCompatActivity() {

    // variáveis para autenticação, banco de dados e funções firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions

    // variáveis para os inputs e botões
    private lateinit var etNumCartao: AppCompatEditText
    private lateinit var etName: AppCompatEditText
    private lateinit var etExpDate: AppCompatEditText
    private lateinit var etSecCode: AppCompatEditText
    private lateinit var btnEnviar: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton

    private lateinit var etNumCartaoError: AppCompatTextView
    private lateinit var etExpDateError: AppCompatTextView
    private lateinit var etSecCodeError: AppCompatTextView
    private lateinit var etNameError: AppCompatTextView


    // classe para representar informações do cartão de crédito
    data class CreditCards(
        val idUser: String? = null,
        val cardNumber: String? = null,
        val cardName: String? = null,
        val expDate: String? = null,
        val secCode: String? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_credit_card)

        // inicialização das instâncias Firebase
        auth = Firebase.auth
        database = Firebase.database.reference
        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions("southamerica-east1")
        val currentUser = auth.currentUser

        // inicialização dos inputs e botões
        etNumCartao = findViewById(R.id.etNumCartao)
        etName = findViewById(R.id.etName)
        etExpDate = findViewById(R.id.etExpDate)
        etSecCode = findViewById(R.id.etSecCode)
        btnEnviar = findViewById(R.id.btnEnviar)

        etNumCartaoError = findViewById(R.id.etNumCartaoError)
        etExpDateError = findViewById(R.id.etExpDateError)
        etSecCodeError = findViewById(R.id.etSecCodeError)
        etNameError = findViewById(R.id.etNameError)

        etExpDate.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(cs: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString() != current) {
                    var clean = s.toString().replace("[^\\d.]|\\.".toRegex(), "")
                    val cleanC = current.replace("[^\\d.]|\\.".toRegex(), "")

                    val cl = clean.length
                    var sel = cl


                    // Limite o tamanho da entrada para 4 caracteres
                    if (cl > 4) {
                        clean = clean.substring(0, 4)
                    }

                    // Adicione um zero à esquerda se o mês for maior que 12
                    val month = if (clean.length >= 2) {
                        val monthStr = clean.substring(0, 2)
                        val monthInt = monthStr.toInt()
                        if (monthInt > 12) {
                            clean = "12" + clean.substring(2)
                            "12"
                        } else {
                            monthStr
                        }
                    } else {
                        clean
                    }

                    // Adicione "/" após o mês
                    if (clean.length >= 2 && clean.length <= 4) {
                        clean = clean.substring(0, 2) + "/" + clean.substring(2)
                    }

                    // Ajuste a posição do cursor
                    sel = if (sel <= 2) sel + 1 else sel + 2


                    // Atualize o campo de texto
                    current = clean
                    etExpDate.setText(current)
                    etExpDate.setSelection(if (sel < current.length) sel else current.length)

                    // Adicione uma saída no Logcat
                    Log.d("TextWatcher", "Texto alterado: $current")
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })



        etNumCartao.filters = arrayOf(InputFilter.LengthFilter(16))
        etSecCode.filters= arrayOf(InputFilter.LengthFilter(3))
        etExpDate.filters = arrayOf(InputFilter.LengthFilter(5))


        // Inicialização do botão "Voltar" aqui, para garantir que ele esteja sempre visível
        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // botão para voltar para a home
            nextScreen(homeScreen::class.java)
        }

        getCreditCardInfos()

        btnEnviar.setOnClickListener {
            // variáveis que recebem dados dos inputs
            val cardNumber = etNumCartao.text.toString()
            val cardName = etName.text.toString()
            val expDate = etExpDate.text.toString()
            val secCode = etSecCode.text.toString()
            val idUser = currentUser?.uid

            // criação da instância card, que recebe dados da classe CreditCard
            val card = CreditCards(idUser, cardNumber, cardName, expDate, secCode)
            CoroutineScope(Dispatchers.Main).launch {
                var isError = false
                if (idUser != null) {
                    if (cardNumber.isEmpty()) {
                        updateInputState(etNumCartao, etNumCartaoError, "Número de cartão inválido", true)
                        isError = true
                    } else if (!isNumeric(cardNumber) || cardNumber.length != 16) {
                        updateInputState(etNumCartao, etNumCartaoError, "Número inválido! Deve conter 16 dígitos", true)
                        isError = true
                    } else {
                        updateInputState(etNumCartao, etNumCartaoError, "", false)
                    }

                    if (cardName.isEmpty()) {
                        updateInputState(etName, etNameError, "Nome inválido", true)
                        isError = true
                    } else if (isNumeric(cardName)) {
                        updateInputState(etName, etNameError, "Nome inválido! Deve conter letras", true)
                        isError = true
                    } else {
                        updateInputState(etName, etNameError, "", false)
                    }

                    if (expDate.isEmpty()) {
                        updateInputState(etExpDate, etExpDateError, "Data inválida", true)
                        isError = true
                    } else if (!isExpirationDateValid(expDate)) {
                        updateInputState(etExpDate, etExpDateError, "Data inválida!", true)
                        isError = true
                    } else {
                        updateInputState(etExpDate, etExpDateError, "", false)
                    }

                    if (secCode.isEmpty()) {
                        updateInputState(etSecCode, etSecCodeError, "CVV inválido", true)
                        isError = true
                    } else if (!isNumeric(secCode) || secCode.length != 3) {
                        updateInputState(etSecCode, etSecCodeError, "Código deve ter 3 dígitos", true)
                        isError = true
                    } else {
                        updateInputState(etSecCode, etSecCodeError, "", false) // Adicionado para limpar mensagens de erro
                    }

                    if (!isError) {
                        // Se não houver erros, chama a função para adicionar o cartão
                        addCreditCard(card)
                    } else {
                        // Se houver mensagens de erro, mostra um snackbar
                        val rootView = findViewById<View>(android.R.id.content)
                        showSnackbar(rootView, "Preencha todos os campos corretamente!", R.color.red)
                    }

                }


            }

        }

    }

    fun showSnackbar(view: View, message: String, backgroundColor: Int) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(view.context.getColor(backgroundColor))
        snackbar.show()
    }

    fun updateInputState(editText: EditText, errorTextView: TextView, errorMessage: String, isError: Boolean) {
        val errorIcon: Drawable? = if (isError) {
            ContextCompat.getDrawable(this, R.drawable.baseline_error_24)?.apply {
                setBounds(-2, 0, intrinsicWidth, intrinsicHeight)
            }
        } else {
            null
        }

        editText.setCompoundDrawables(null, null, errorIcon, null)

        if (isError) {
            editText.setBackgroundResource(R.drawable.error_edit_text_background)
            errorTextView.text = errorMessage
            errorTextView.visibility = View.VISIBLE
        } else {
            editText.setBackgroundResource(R.drawable.email_login)
            errorTextView.visibility = View.GONE
        }
    }


    private fun getCreditCardInfos() {
        val firestore = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser?.uid

        if (currentUser != null) {
            firestore.collection("CreditCards")
                .whereEqualTo("idUser", currentUser)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val cardInfo = document.toObject(CreditCards::class.java)
                        if (cardInfo != null) {
                            // Preenche os campos com as informações do cartão
                            etNumCartao.setText(cardInfo.cardNumber ?: "")
                            etName.setText(cardInfo.cardName ?: "")
                            etExpDate.setText(cardInfo.expDate ?: "")
                            etSecCode.setText(cardInfo.secCode ?: "")

                            // Habilita a edição dos campos
                            etNumCartao.isEnabled = false
                            etName.isEnabled = false
                            etExpDate.isEnabled = false
                            etSecCode.isEnabled = false

                            btnEnviar.setBackgroundResource(R.color.dark_grey)
                            btnEnviar.isEnabled = false

                        } else {
                            Log.e("error", "Os dados do cartão de crédito estão vazios.")
                        }
                    } else {
                        Log.d("debugg", "Nenhum cartão de crédito encontrado para este usuário.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("DataScreen", "Erro ao recuperar dados do Firestore: $exception")
                    Toast.makeText(this, "Erro, tente de novo mais tarde", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("error", "Usuário atual é nulo.")
            Toast.makeText(this, "Erro: usuário não autenticado.", Toast.LENGTH_SHORT).show()
        }
    }


    // função que adiciona o cartão no banco de dados
    private fun addCreditCard(card: CreditCards) {
        // faz um map nos dados do cartão
        val cc = hashMapOf(
            "idUser" to card.idUser,
            "cardName" to card.cardName,
            "cardNumber" to card.cardNumber,
            "secCode" to card.secCode,
            "expDate" to card.expDate
        )
        // chama a função do Firebase para cadastrar o usuário
        functions.getHttpsCallable("addCreditCard").call(cc)
            .addOnSuccessListener { result ->
                // Sucesso ao cadastrar o usuário
                val response = result.data as HashMap<*, *>
                Log.d(ContentValues.TAG, "Cartão de crédito cadastrado com sucesso")
                Toast.makeText(this, "Cartão de Crédito cadastrado com sucesso.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Erro ao cadastrar o usuário
                Log.w(ContentValues.TAG, "Erro ao cadastrar Cartão de crédito", e)
                Toast.makeText(this, "Erro, tente de novo  mais tarde", Toast.LENGTH_SHORT).show()
            }
    }

    // função generica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val loginScreen = Intent(this, screen)
        startActivity(loginScreen)

    }

    private fun isExpirationDateValid(expDate: String): Boolean {
        val sdf = SimpleDateFormat("MM/yy", Locale.getDefault())
        sdf.isLenient = false

        return try {
            val expirationDate = sdf.parse(expDate)
            if (expirationDate != null) {
                // Obtém o primeiro dia do mês de expiração
                val calendar = Calendar.getInstance()
                calendar.time = expirationDate
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

                // Verifica se a data de expiração é após a data atual
                val currentDate = Calendar.getInstance().time
                expirationDate.after(currentDate)
            } else {
                false
            }
        } catch (e: ParseException) {
            Log.e("DateError", "Erro ao analisar a data de expiração: $expDate")
            false
        }
    }





    // Função que verifica se uma string contém apenas dígitos
    private fun isNumeric(str: String): Boolean {
        return str.all { it.isDigit() }
    }

}