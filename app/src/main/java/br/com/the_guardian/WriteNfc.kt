package br.com.the_guardian

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import java.io.IOException

class WriteNfc : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var qrCodeContent: String? = null
    private lateinit var btnVoltar: AppCompatButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_nfc)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Por favor, ative o NFC nas configurações do seu aparelho", Toast.LENGTH_SHORT).show()
        } else {
            // Recupera os dados escaneados do Intent
            qrCodeContent = QrCodeData.scannedData
            Log.e("debug", qrCodeContent.toString())
        }

        // Inicialização do botão "Voltar" aqui, para garantir que ele esteja sempre visível
        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // botão para voltar para a home
            nextScreen(HomeGerente::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("debug", "passei o super")
        val action = intent.action
        Log.d("debug", "Ação do intent: $action")

        if (NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            Log.d("debug", "Ação encontrada")
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            Log.e("debug", tag.toString())
            if (tag != null) {
                Log.d("debug", qrCodeContent.toString())
                writeNfcTag(qrCodeContent.toString(), tag)
            } else {
                Toast.makeText(this, "Nenhuma tag NFC encontrada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeNfcTag(uid: String, tag: Tag) {
        Log.d("debug", "Entrou no writeNfcTag")
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                val textRecord = NdefRecord.createTextRecord(null, uid)
                val ndefMessage = NdefMessage(arrayOf(textRecord))

                ndef.connect()
                Log.d("debug", ndefMessage.toString())
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()

                Toast.makeText(this, "Locação realizada com sucesso", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "A tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao escrever na tag NFC: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro desconhecido ao escrever na tag NFC", Toast.LENGTH_SHORT).show()
        }
    }

    // função generica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val HomeGerente = Intent(this, screen)
        startActivity(HomeGerente)

    }
}
