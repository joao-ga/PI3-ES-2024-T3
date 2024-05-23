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
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException

class WriteNfc : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var qrCodeContent: String? = null
    private lateinit var btnVoltar: AppCompatButton
    private lateinit var db: FirebaseFirestore



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_nfc)
        db = FirebaseFirestore.getInstance()


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

                getlocker(qrCodeContent)


            } else {
                Toast.makeText(this, "Nenhuma tag NFC encontrada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeNfcTag(uid: String, tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                val textRecord = NdefRecord.createTextRecord(null, uid)
                val ndefMessage = NdefMessage(arrayOf(textRecord))

                ndef.connect()
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
            Toast.makeText(this, "Erro ao escrever na tag NFC", Toast.LENGTH_SHORT).show()
        }
    }

    // função generica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val HomeGerente = Intent(this, screen)
        startActivity(HomeGerente)

    }
    private fun getlocker(uid: String?) {
        db.collection("Locations")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Nenhum armário encontrado para este UID", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in documents) {
                        val locker = document.getString("locker")
                        if (locker != null) {
                            Toast.makeText(this, "Armário alugado: $locker", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Dados do armário não encontrados", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("debug", "Erro ao recuperar dados: ", exception)
                Toast.makeText(this, "Erro ao recuperar dados do armário", Toast.LENGTH_SHORT).show()
            }
    }
}
