package br.com.the_guardian

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.nio.charset.Charset

class ReadNfc : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_read_nfc)

        // Inicializa o NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Verifica se o NFC está habilitado no dispositivo
        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Por favor, ative o NFC nas configurações do seu aparelho", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        // Configura um PendingIntent para que esta atividade seja acionada quando uma tag NFC for detectada
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Configura os filtros de intenção para processar somente ações relacionadas a NFC
        val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        // Registra os filtros de intenção e o PendingIntent
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()

        // Desabilita o envio em primeiro plano para evitar o consumo de energia quando a atividade não está em foco
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Verifica se a intent contém uma tag NFC
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            // Extrai as mensagens NDEF da intent
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null) {
                val ndefMessages = rawMessages.mapNotNull { it as? NdefMessage }
                if (ndefMessages.isNotEmpty()) {
                    // Percorre todas as mensagens NDEF
                    for (message in ndefMessages) {
                        // Percorre todos os registros de cada mensagem NDEF
                        for (record in message.records) {
                            // Extrai os bytes do payload do registro
                            val payload = record.payload

                            // Converte os bytes do payload para uma string
                            val payloadString = String(payload, Charset.forName("UTF-8"))

                            // Divide a string pelo delimitador '$'
                            val parts = payloadString.split('$')

                            // Se houver pelo menos duas partes
                            if (parts.size >= 2) {
                                // A segunda parte é a imagem em base64
                                val imageBase64 = parts[1]
                                // Decodifica a string base64 para obter os bytes da imagem
                                val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)

                                // Converte os bytes da imagem de volta para um bitmap
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                                // Passa o bitmap para a atividade ConfirmarUsuario
                                val confirmIntent = Intent(this, ConfirmarUsuario::class.java)
                                confirmIntent.putExtra("imageBitmap", bitmap)
                                startActivity(confirmIntent)
                            } else {
                                // Se não houver delimitador, significa que os dados na tag NFC estão em um formato incorreto
                                Toast.makeText(this, "Formato de dados NFC incorreto", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Nenhuma mensagem NDEF encontrada", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Nenhuma mensagem NDEF encontrada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}