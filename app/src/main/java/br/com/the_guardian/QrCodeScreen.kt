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
            deleteLocationInfo()
        }

        btnVoltar.setOnClickListener {
            nextScreen(homeScreen::class.java)
        }

        // referência para o ImageView onde o QR code será exibido
        val qrCode = findViewById<ImageView>(R.id.qrCode)

        // Gera um código do id do usuário + o id do armário + o preço + a hora de inicio da locação
        var idUsuario = auth.currentUser?.uid


        // converte o código em um QR code e o exibe no ImageView
        val text = "$idUsuario"
        try {
            val bitmap = textToImageEncode(text)
            qrCode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }

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

    // funcao que delete uma locação
    private fun deleteLocationInfo() {
        val currentUser = auth.currentUser?.uid
        if (currentUser != null) {
            // busca no banco uma locação pelo uid do usuario
            db.collection("Locations").whereEqualTo("uid", currentUser)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        document.reference.delete()
                            .addOnSuccessListener {
                                // deixa a locação como falsa e muda de tela
                                locacaoConfirmada =  false
                                nextScreen(homeScreen::class.java)
                                Toast.makeText(this, "Pendência de locação cancelada!", Toast.LENGTH_SHORT).show()
                                Log.d("debugg", "Documento excluído com sucesso")
                            }
                            // logs de erro
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Erro em cancelar pendência, tente de novo mais tarde!", Toast.LENGTH_SHORT).show()
                                Log.d(ContentValues.TAG, "Falha ao excluir o documento do usuário:", exception)
                            }
                    } else {
                        Log.d(ContentValues.TAG, "Documento do usuário não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(ContentValues.TAG, "Falha ao obter o documento do usuário:", exception)
                }
        }
    }

    // função generica que muda de tela
    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }
}