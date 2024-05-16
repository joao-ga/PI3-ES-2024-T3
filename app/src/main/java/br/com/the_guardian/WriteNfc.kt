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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.nio.charset.Charset

class WriteNfc : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var qrCodeContent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_write_nfc)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Por favor, ative o NFC nas configurações do seu aparelho", Toast.LENGTH_SHORT).show()
        } else {
            qrCodeContent = intent.getStringExtra("QR_CODE_CONTENT")
        }
    }

    override fun onResume() {
        super.onResume()
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT
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
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (tag != null && qrCodeContent != null) {
                writeNfcTag(qrCodeContent!!, tag)
            } else {
                Toast.makeText(this, "Nenhuma tag NFC ou conteúdo do QR Code encontrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeNfcTag(qrCodeContent: String, tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                val mimeRecord = NdefRecord.createMime("text/plain", qrCodeContent.toByteArray(Charset.defaultCharset()))
                val ndefMessage = NdefMessage(mimeRecord)
                ndef.connect()
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                Toast.makeText(this, "Dados do QR Code escritos na tag NFC com sucesso", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "A tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao escrever na tag NFC", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro desconhecido ao escrever na tag NFC", Toast.LENGTH_SHORT).show()
        }
    }
}
