package br.com.the_guardian

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import kotlin.random.Random

class QrCodeScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_screen)

        val checkedRadioButtonText = intent.getStringExtra("checkedRadioButtonText")
        if (checkedRadioButtonText != null) {

            val qrCode = findViewById<ImageView>(R.id.qrCode)
            val codigo = Random.nextInt(9999)
            val text = codigo
            try {
                val bitmap = textToImageEncode(text)
                qrCode.setImageBitmap(bitmap)
            } catch (e: WriterException) {
                e.printStackTrace()
            }

            var textCode = findViewById<TextView>(R.id.codigo)
            textCode.text = text.toString()
        }
    }

    @Throws(WriterException::class)
    private fun textToImageEncode(value: Int): Bitmap? {
        val bitMatrix: BitMatrix
        try {
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
        val bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444)
        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight)
        return bitmap

    }

}