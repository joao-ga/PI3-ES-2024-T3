package br.com.the_guardian

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
        integrator.setPrompt("ESCANEIE o QRcode")
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

                    // Chama o método para exibir o diálogo de seleção de pessoas
                    showSelectPersonDialog()

                } else {
                    Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }

    // Método para exibir o diálogo de escolha do número de pessoas
    @SuppressLint("MissingInflatedId")
    private fun showSelectPersonDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_person, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()



        dialogView.findViewById<Button>(R.id.btnOnePerson).setOnClickListener {
            //nextScreen(cameraScreen::class.java)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnTwoPerson).setOnClickListener {
            //nextScreen(cameraScreen::class.java)
            dialog.dismiss()
        }

        dialog.show()
    }
}
