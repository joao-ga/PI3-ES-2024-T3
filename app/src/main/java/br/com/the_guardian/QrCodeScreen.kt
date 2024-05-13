package br.com.the_guardian

// importações
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import br.com.the_guardian.DataScreen.Companion.locacaoConfirmada
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix


class QrCodeScreen : AppCompatActivity() {

    private lateinit var btnCancelarloc: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_screen)

        // Inicialização da FirebaseAuth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnCancelarloc = findViewById(R.id.btnCancelarloc)
        btnVoltar = findViewById(R.id.btnVoltar)

        btnCancelarloc.setOnClickListener{
            atualizarStatusLocacaoUsuario()
        }

        btnVoltar.setOnClickListener {
            nextScreen(homeScreen::class.java)
        }

        // referência para o ImageView onde o QR code será exibido
        val qrCode = findViewById<ImageView>(R.id.qrCode)

        // Gera um código do id do usuário + o id do armário
        var idUsuario = auth.currentUser?.uid
        var idArmario = intent.getStringExtra("idArmario")

        // converte o código em um QR code e o exibe no ImageView
        val text = "$idUsuario$idArmario"
        try {
            val bitmap = textToImageEncode(text)
            qrCode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    // função para converter um texto em um QR code na forma de Bitmap
    @Throws(WriterException::class)
    private fun textToImageEncode(value: String): Bitmap? {
        val bitMatrix: BitMatrix
        try {
            // codifica o texto em um QR code
            bitMatrix = MultiFormatWriter().encode(
                value.toString(),
                BarcodeFormat.QR_CODE,
                500, 500, null
            )
        } catch (illegalArgumentException: IllegalArgumentException) {
            return null
        }
        val bitMatrixWidth = bitMatrix.width
        val bitMatrixHeight = bitMatrix.height
        val pixels = IntArray(bitMatrixWidth * bitMatrixHeight)
        for (y in 0 until bitMatrixHeight) {
            val offset = y * bitMatrixWidth
            for (x in 0 until bitMatrixWidth) {
                pixels[offset + x] = if (bitMatrix[x, y]) -0x1000000 else -0x1
            }
        }
        // cria um Bitmap a partir dos pixels do QR code e retorna
        val bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444)
        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight)

        return bitmap
    }

    private fun atualizarStatusLocacaoUsuario() {
        Log.d("debugg", "entrou na funcao atualizarStatus")
        val currentUser = auth.currentUser?.uid
        if (currentUser != null) {
            db.collection("Users")
                .whereEqualTo("uid", currentUser)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        db.collection("Users")
                            .document(document.id)
                            .update("hasLocker", false)
                            .addOnSuccessListener {
                                locacaoConfirmada =  false
                                nextScreen(homeScreen::class.java)
                                Toast.makeText(this, "Pendência de locação cancelada!", Toast.LENGTH_SHORT).show()
                                Log.d(ContentValues.TAG, "Status de locação atualizado com sucesso!")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Erro em cancelar pendência, tente de novo mais tarde!", Toast.LENGTH_SHORT).show()
                                Log.w(ContentValues.TAG, "Erro ao atualizar o status de locação", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(ContentValues.TAG, "Erro ao obter documentos", e)
                }
        }
    }

    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }

}