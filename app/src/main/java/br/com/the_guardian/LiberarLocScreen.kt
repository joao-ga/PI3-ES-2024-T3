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

    private fun openCamera() {
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false)
        integrator.setPrompt("ESCANEIE o QRcode")
        integrator.setBeepEnabled(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val contents = result.contents
              
                QrCodeData.scannedData = contents // Salva os dados escaneados na variável

                scannedData = contents
              
                Log.i("CONTENT SCAN", contents)
                showSelectPersonDialog()
            } else {
                Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSelectPersonDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_person, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnOnePerson).setOnClickListener {
            numberOfPersons = 1
            startCameraActivity()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnTwoPerson).setOnClickListener {
            numberOfPersons = 2
            startCameraActivity()
            dialog.dismiss()
        }

        dialogView.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra("NUMBER_PERSON", numberOfPersons)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    // função generica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val HomeGerente = Intent(this, screen)
        startActivity(HomeGerente)

    }

    private fun startNfcWriteActivity() {
        val intent = Intent(this, WriteNfc::class.java)
        intent.putExtra("QR_CODE_CONTENT", scannedData)
        startActivity(intent)
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }
}
