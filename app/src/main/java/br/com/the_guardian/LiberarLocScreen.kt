package br.com.the_guardian

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator


object QrCodeData {
    var scannedData: String? = null
}
class LiberarLocScreen : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liberar_loc_screen)
        openCamera()
    }

    private fun openCamera() {
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false) // Permitir rotação
        integrator.setPrompt("ESCANEIE o QRcode")
        integrator.setBeepEnabled(false) // Desativar som de beep
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val contents = result.contents
                QrCodeData.scannedData = contents // Salva os dados escaneados na variável
                Toast.makeText(this, contents, Toast.LENGTH_LONG).show()
                Log.i("CONTENT SCAN", contents)
                showSelectPersonDialog()
            } else {
                Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        newScreen.putExtra("QR_CODE_CONTENT", QrCodeData.scannedData) // Adiciona os dados escaneados como extra
        startActivity(newScreen)
    }

    private fun showSelectPersonDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_person, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnOnePerson).setOnClickListener {
            nextScreen(CameraActivity::class.java)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnTwoPerson).setOnClickListener {
            nextScreen(CameraActivity::class.java)
            dialog.dismiss()
        }

        dialog.show()  // Certifique-se de exibir o diálogo aqui
    }
}
