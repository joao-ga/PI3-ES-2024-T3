package br.com.the_guardian

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.zxing.integration.android.IntentIntegrator


object QrCodeData {
    var scannedData: String? = null
}
class LiberarLocScreen : AppCompatActivity() {

    private var scannedData: String? = null
    private var numberOfPersons: Int = 0
    private lateinit var btnVoltar: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liberar_loc_screen)
        openCamera()

        // Inicialização do botão "Voltar" aqui, para garantir que ele esteja sempre visível
        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // botão para voltar para a home
            nextScreen(HomeGerente::class.java)
        }

    }

    // função que abre a camera
    private fun openCamera() {
        val integrator = IntentIntegrator(this)
        // orientação da camera
        integrator.setOrientationLocked(false)
        // testo de referência para o usuario
        integrator.setPrompt("ESCANEIE o QRcode")
        integrator.setBeepEnabled(false)
        // realiza o scan
        integrator.initiateScan()
    }

    // funbção que é chamada ao realizar scan
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val contents = result.contents

                // Salva os dados escaneados na variável
                QrCodeData.scannedData = contents
                scannedData = contents

                // aparece o dialod para ver a quantidade de pessoas que vão tirar foto
                showSelectPersonDialog()
            } else {
                // log de erro
                Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSelectPersonDialog() {
        // Infla a visualização do layout do diálogo
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_person, null)

        // Cria um diálogo com o layout inflado
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Define o comportamento do botão para selecionar uma pessoa
        dialogView.findViewById<Button>(R.id.btnOnePerson).setOnClickListener {
            // Define o número de pessoas para 1
            numberOfPersons = 1
            // Inicia a atividade da câmera
            startCameraActivity()
            // Fecha o diálogo
            dialog.dismiss()
        }

        // Define o comportamento do botão para selecionar duas pessoas
        dialogView.findViewById<Button>(R.id.btnTwoPerson).setOnClickListener {
            // Define o número de pessoas para 2
            numberOfPersons = 2
            // Inicia a atividade da câmera
            startCameraActivity()
            // Fecha o diálogo
            dialog.dismiss()
        }

        // Define o comportamento do botão para fechar o diálogo
        dialogView.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            // Fecha o diálogo
            dialog.dismiss()
        }
        // Exibe o diálogo
        dialog.show()
    }

    private fun startCameraActivity() {
        // Cria uma intenção para iniciar a atividade da câmera
        val intent = Intent(this, CameraActivity::class.java)
        // Adiciona um extra na intenção com o número de pessoas selecionado
        intent.putExtra("NUMBER_PERSON", numberOfPersons)
        // Inicia a atividade da câmera e aguarda um resultado
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    // Função genérica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        // Cria uma intenção para iniciar a nova tela (atividade)
        val HomeGerente = Intent(this, screen)
        // Inicia a nova atividade
        startActivity(HomeGerente)
    }

    private fun startNfcWriteActivity() {
        // Cria uma intenção para iniciar a atividade de escrita NFC
        val intent = Intent(this, WriteNfc::class.java)
        // Adiciona um extra na intenção com o conteúdo do código QR escaneado
        intent.putExtra("QR_CODE_CONTENT", scannedData)
        // Inicia a atividade de escrita NFC
        startActivity(intent)
    }

    companion object {
        // Constante para identificar a solicitação de captura de imagem
        const val REQUEST_IMAGE_CAPTURE = 1
    }
}
