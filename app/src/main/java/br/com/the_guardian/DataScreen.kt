package br.com.the_guardian

// importações
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

data class Locacao(
    val userId: String,
    val userLoc: String?,
    val priceSelected: Double
)

class DataScreen : AppCompatActivity() {

    // variáveis para os botões
    private lateinit var btnConsultar: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton

    private lateinit var auth: FirebaseAuth
    private var locacaoAtual: Locacao? = null
    private lateinit var userId: String // Movendo a declaração para o escopo adequado

    private fun confirmacao(locacao: Locacao) {
        Toast.makeText(this, "Locação confirmada: $locacao", Toast.LENGTH_SHORT).show()
    }

    companion object {
        var locacoesConfirmadas = mutableListOf<Locacao>()
        // Variável global para indicar se uma locação foi confirmada, usa-la para mandar o usuario direto para tela do qrcode
        var locacaoConfirmada: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_screen)

        // Inicialização da FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Obtendo o usuário atual
        userId = auth.currentUser?.uid ?: ""

        // recuperar os dados do lugar referênciado e os preços do intent
        val name = intent.getStringExtra("name")
        val reference = intent.getStringExtra("reference")
        val disponibility = intent.getBooleanExtra("disponibility", false)
        val prices = intent.getIntArrayExtra("prices")

        // atualizar a interface do usuário com os dados recuperados
        findViewById<TextView>(R.id.marker_title).text = "Alugar $name"
        findViewById<TextView>(R.id.marker_reference).text = reference
        findViewById<TextView>(R.id.marker_disponibility).text = if (disponibility) "Está disponível: Sim" else "Está disponível: Não"

        // configuração dos botões de preço
        val radioButtons = listOf(
            findViewById<RadioButton>(R.id.radiobutton1),
            findViewById(R.id.radiobutton2),
            findViewById(R.id.radiobutton3),
            findViewById(R.id.radiobutton4),
            findViewById(R.id.radiobutton5)
        )

        prices?.forEachIndexed { index, price ->
            if (index < radioButtons.size) {
                radioButtons[index].text = "R$ $price"
            }
        }

        // configuração da disponibilidade e cor dos botões de preço com base na disponibilidade
        radioButtons.forEach { radioButton ->
            radioButton.isEnabled = disponibility
            radioButton.setTextColor(if (disponibility) Color.BLACK else Color.GRAY)
        }

        // verifica a hora atual para habilitar ou desabilitar o botão do dia inteiro
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        if (hour in 6..8) {
            radioButtons.last().isEnabled = true
        } else {
            radioButtons.last().isEnabled = false
            radioButtons.last().setTextColor(Color.GRAY)
        }

        // Configurar o botão Voltar para fechar a tela
        btnVoltar = findViewById(R.id.btn_voltar)
        btnVoltar.setOnClickListener {
            finish()
        }

        // Configurar o botão Consultar para iniciar a tela de QrCode
        btnConsultar = findViewById(R.id.btn_consultar)
        btnConsultar.isEnabled = disponibility
        btnConsultar.setOnClickListener {
            var isAnyRadioButtonChecked = false
            var checkedRadioButtonId = -1

            // verificação se algum botão de preço foi selecionado
            for (radioButton in radioButtons) {
                if (radioButton.isChecked) {
                    isAnyRadioButtonChecked = true
                    checkedRadioButtonId = radioButton.id
                    break
                }
            }

            // se algum botão foi selecionado, inicia a tela de QrCode com o preço selecionado
            if (isAnyRadioButtonChecked) {
                if(usuarioEstaLogado()) {
                    if(locacaoConfirmada) {
                        if (locacaoAtual == null) {
                            val precoSelecionadoText = findViewById<RadioButton>(checkedRadioButtonId).text.toString()
                            val precoNumerico = precoSelecionadoText.substringAfter("R$ ").toDoubleOrNull()
                            if (precoNumerico != null) {
                                val userLoc = name
                                val precoSelecionado = precoNumerico
                                locacaoAtual = Locacao(userId, userLoc, precoSelecionado)
                                locacoesConfirmadas.add(locacaoAtual!!)
                                locacaoConfirmada = true // Atualizando a variável global para indicar que a locação foi confirmada
                                confirmacao(locacaoAtual!!)
                                val intent = Intent(this, QrCodeScreen::class.java).apply {
                                    putExtra("checkedRadioButtonText", precoSelecionadoText)
                                }
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "Preço selecionado inválido", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Você já possui uma locação confirmada.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Você já possui uma locação confirmada.", Toast.LENGTH_SHORT).show()

                    }
                } else {
                    Toast.makeText(baseContext, "Para acessar essa funcionalidade, faça o login", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Escolha uma opção", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun usuarioEstaLogado(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val usuarioAtual = auth.currentUser
        return usuarioAtual != null
    }

}
