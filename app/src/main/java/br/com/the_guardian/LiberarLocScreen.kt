package br.com.the_guardian

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.zxing.integration.android.IntentIntegrator

class LiberarLocScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liberar_loc_screen)

        openCamera()

    }



    private val REQUEST_CODE_SCAN = 0

    private fun openCamera() {
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false) // Permitir rotação
        integrator.setPrompt("Aponte a câmera para o QR Code")
        integrator.setBeepEnabled(false) // Desativar som de beep
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCAN) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents != null) {
                    val contents = result.contents
                    Toast.makeText(this, contents, Toast.LENGTH_LONG).show()
                    Log.i("CONTENT SCAN ", contents)
                } else {
                    Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}