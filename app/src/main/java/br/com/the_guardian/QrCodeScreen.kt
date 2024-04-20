package br.com.the_guardian

// importações
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import kotlin.random.Random


class QrCodeScreen : AppCompatActivity() {

    // declarando variaveis uteis para o sistema
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

        // iniciando os botoes
        btnCancelarloc = findViewById(R.id.btnCancelarloc)
        btnVoltar = findViewById(R.id.btnVoltar)

        // botao que chama funcao que cancela a pendencia de locacao
        btnCancelarloc.setOnClickListener{
            atualizarStatusLocacaoUsuario()
        }

        // botao que volta para a home
        btnVoltar.setOnClickListener {
            nextScreen(homeScreen::class.java)
        }

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

    // funcao que atualiza o status do campo hasLocker
    private fun atualizarStatusLocacaoUsuario() {
        // aramazena o uid do usuario
        val currentUser = auth.currentUser?.uid
        if (currentUser != null) {
            // se ele nao for vazio abre a colecao Users
            db.collection("Users")
                //faz a busca do documento pelo uid do usuario
                .whereEqualTo("uid", currentUser)
                .get()
                .addOnSuccessListener { documents ->
                    // recebe os dados e faz um for para ir ate encontrar o campo hasLocker
                    for (document in documents) {
                        db.collection("Users")
                            .document(document.id)
                            // atribue o valor false para o campo hasLocker
                            .update("hasLocker", false)
                            .addOnSuccessListener {
                                //caso for sucesso mudar para a tela home e avisar o usuario
                                nextScreen(homeScreen::class.java)
                                Toast.makeText(this, "Pendência de locação cancelada!", Toast.LENGTH_SHORT).show()
                                Log.d(ContentValues.TAG, "Status de locação atualizado com sucesso!")
                            }
                            .addOnFailureListener { e ->
                                // se der erro avisa o usurio para tentar mais tarde
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

    // funcao generica que muda de screen
    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }

}