package br.com.the_guardian

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
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
            // Extrai a tag NFC da intent
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            // Verifica se a tag é válida
            if (tag != null) {
                // Lê os dados da tag NFC
                val ndef = Ndef.get(tag)
                if (ndef != null) {
                    ndef.connect()

                    // Lê a mensagem NDEF da tag
                    val ndefMessage = ndef.ndefMessage
                    if (ndefMessage != null) {
                        // Itera sobre os registros da mensagem NDEF
                        for (record in ndefMessage.records) {
                            // Converte o payload do registro para uma string
                            val payloadString = String(record.payload, Charset.forName("UTF-8"))

                            // Armazena o UID lido
                            val uid = payloadString

                            // passar para a próxima atividade
                            val confirmIntent = Intent(this, ConfirmarUsuario::class.java)
                                confirmIntent.putExtra("uid", uid)
                                startActivity(confirmIntent)


                            // Exemplo: exibir o UID em um Toast
                            Toast.makeText(this, "UID lido da tag NFC: $uid", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Nenhuma mensagem NDEF encontrada na tag NFC", Toast.LENGTH_SHORT).show()
                    }

                    ndef.close()
                } else {
                    Toast.makeText(this, "A tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Tag NFC inválida", Toast.LENGTH_SHORT).show()
            }
        }
    }

}