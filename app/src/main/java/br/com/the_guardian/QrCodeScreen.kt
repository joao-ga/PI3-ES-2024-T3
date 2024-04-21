package br.com.the_guardian

// importações
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import kotlin.random.Random

class QrCodeScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_screen)
        // recupera o texto do código do intent


            // referência para o ImageView onde o QR code será exibido
            val qrCode = findViewById<ImageView>(R.id.qrCode)

            // Gera um código aleatório
            var codigo = 0
            while(codigo < 1000){
                codigo = Random.nextInt(9999)
            }

            // converte o código em um QR code e o exibe no ImageView
            val text = codigo
            try {
                val bitmap = textToImageEncode(text)
                qrCode.setImageBitmap(bitmap)
            } catch (e: WriterException) {
                e.printStackTrace()
            }


    }

    // função para converter um texto em um QR code na forma de Bitmap
    @Throws(WriterException::class)
    private fun textToImageEncode(value: Int): Bitmap? {
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
}