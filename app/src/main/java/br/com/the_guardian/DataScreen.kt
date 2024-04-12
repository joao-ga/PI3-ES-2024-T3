package br.com.the_guardian


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import java.util.Calendar

class DataScreen : AppCompatActivity() {

    private lateinit var btnConsultar: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_screen)

        // Recuperar os dados do lugar clicado e os preços do intent
        val name = intent.getStringExtra("name")
        val reference = intent.getStringExtra("reference")
        val disponibility = intent.getBooleanExtra("disponibility", false)
        val prices = intent.getIntArrayExtra("prices")

        // Atualizar a interface do usuário com os dados recuperados
        findViewById<TextView>(R.id.marker_title).text = "Alugar $name"
        findViewById<TextView>(R.id.marker_reference).text = reference
        findViewById<TextView>(R.id.marker_disponibility).text = if (disponibility) "Está disponível: Sim" else "Está disponível: Não"

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

        radioButtons.forEach { radioButton ->
            radioButton.isEnabled = disponibility
            radioButton.setTextColor(if (disponibility) Color.rgb(160,228,24) else Color.rgb(217,217,217))
        }

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        if(hour in 6..8){
            radioButtons.last().isEnabled = true
        }
        else{
            radioButtons.last().isEnabled = false
            radioButtons.last().setTextColor(Color.GRAY)
        }

        // Configurar o botão Voltar para fechar a tela
        btnVoltar = findViewById(R.id.btn_voltar)
        btnVoltar.setOnClickListener{
            finish()
        }

        // Configurar o botão Consultar para iniciar a tela de QrCode
        btnConsultar = findViewById(R.id.btn_consultar)
        btnConsultar.isEnabled = disponibility
        btnConsultar.setOnClickListener {
            var isAnyRadioButtonChecked = false
            var checkedRadioButtonId = -1

            for (radioButton in radioButtons) {
                if (radioButton.isChecked) {
                    isAnyRadioButtonChecked = true
                    checkedRadioButtonId = radioButton.id
                    break
                }
            }

            if (isAnyRadioButtonChecked) {
                val intent = Intent(this, QrCodeScreen::class.java).apply {
                    putExtra("checkedRadioButtonText", findViewById<RadioButton>(checkedRadioButtonId).text.toString())
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Escolha uma opção", Toast.LENGTH_SHORT).show()
            }

        }
    }

}
