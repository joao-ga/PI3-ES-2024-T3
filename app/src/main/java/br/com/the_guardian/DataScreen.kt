package br.com.the_guardian


import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioButton
import android.widget.TextView
import android.graphics.Color
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton

class DataScreen : AppCompatActivity() {

    private lateinit var btnConsultar: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton

    @SuppressLint("SetTextI18n")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_screen)

        val name = intent.getStringExtra("name")
        val reference = intent.getStringExtra("reference")
        val disponibility = intent.getBooleanExtra("disponibility", false)

        findViewById<TextView>(R.id.marker_title).text = "Alugar $name"
        findViewById<TextView>(R.id.marker_reference).text = reference
        findViewById<TextView>(R.id.marker_disponibility).text = if (disponibility) "Está disponível: Sim"
        else "Está disponível: Não"

        val radiobutton1 = findViewById<RadioButton>(R.id.radiobutton1)
        val radiobutton2 = findViewById<RadioButton>(R.id.radiobutton2)
        val radiobutton3 = findViewById<RadioButton>(R.id.radiobutton3)
        val radiobutton4 = findViewById<RadioButton>(R.id.radiobutton4)
        val radiobutton5 = findViewById<RadioButton>(R.id.radiobutton5)

        val radioButtons = listOf(radiobutton1, radiobutton2, radiobutton3, radiobutton4, radiobutton5)

        radioButtons.forEach { radioButton ->
            radioButton.isEnabled = disponibility
            radioButton.setTextColor(if (disponibility) Color.BLACK else Color.GRAY)
        }

        btnVoltar = findViewById(R.id.btn_voltar)
        btnVoltar.setOnClickListener{
            finish()
        }

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