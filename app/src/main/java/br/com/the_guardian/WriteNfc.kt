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
import androidx.appcompat.app.AppCompatActivity
import br.com.the_guardian.R
import java.io.IOException
import java.nio.charset.Charset

data class NfcData(val picture: String?, val qrCodeContent: String?)

class WriteNfc : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var qrCodeContent: String? = null
    private var nfcData: NfcData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_nfc)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Por favor, ative o NFC nas configurações do seu aparelho", Toast.LENGTH_SHORT).show()
        } else {
            qrCodeContent = intent.getStringExtra("QR_CODE_CONTENT")
        }

        val picture = intent.getStringExtra("CURRENT_PERSON")

        nfcData = NfcData(picture, qrCodeContent)
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
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null) {
                val ndefMessages = rawMessages.mapNotNull { it as? NdefMessage }
                if (ndefMessages.isNotEmpty()) {
                    for (message in ndefMessages) {
                        for (record in message.records) {
                            val tag = record.toByteArray()
                            nfcData?.let { nfcData ->
                                writeNfcTag(nfcData, tag)
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

    private fun writeNfcTag(nfcData: NfcData, tag: ByteArray) {
        try {
            val nfcTag: Tag = tag as Tag
            val ndef = Ndef.get(nfcTag)
            if (ndef != null) {
                val combinedData = "${nfcData.picture},${nfcData.qrCodeContent}".toByteArray(Charset.defaultCharset())
                val mimeRecord = NdefRecord.createMime("text/plain", combinedData)
                val ndefMessage = NdefMessage(mimeRecord)

                ndef.connect()
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()

                Toast.makeText(this, "Dados do QR Code escritos na tag NFC com sucesso", Toast.LENGTH_SHORT).show()
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
}
