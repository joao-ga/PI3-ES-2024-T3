package br.com.the_guardian

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.the_guardian.databinding.ActivityReadNfcBinding
import java.nio.charset.Charset

class ReadNfc : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var binding: ActivityReadNfcBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadNfcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
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

        val bundle = Bundle()
        bundle.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

        // Registra o modo leitor para processar NFC tags
        try {
            nfcAdapter?.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, bundle)
        } catch (e: IllegalStateException) {
            Log.e("NFC", "Erro ao habilitar reader mode", e)
        }
    }

    override fun onPause() {
        super.onPause()

        // Desabilita o modo leitor para evitar o consumo de energia quando a atividade não está em foco
        try {
            nfcAdapter?.disableReaderMode(this)
        } catch (e: IllegalStateException) {
            Log.e("NFC", "Erro ao desabilitar reader mode", e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onTagDiscovered(tag: Tag?) {
        tag?.let {
            val ndef = Ndef.get(it)
            ndef?.let { ndef ->
                try {
                    ndef.connect()
                    val ndefMessage = ndef.ndefMessage
                    Log.d("debug", ndefMessage.toString())
                    ndef.close()

                    val informacoes = ndefMessage.records
                    if (informacoes.isNotEmpty()) {
                        val firstRecord = informacoes[0]
                        val payload = firstRecord.payload
                        val text = String(payload, Charset.forName("UTF-8"))
                        val uid = text.substring(3)
                        Log.d("NFC", "Tag detectada: $uid")

                        runOnUiThread {
                            Log.d("NFC", "Iniciando ConfirmarUsuario Activity com uid: $uid")
                            // Iniciar ConfirmarUsuario Activity com os dados da tag
                            startActivity(Intent(this, ConfirmarUsuario::class.java).apply {
                                putExtra("uid", uid)
                            })
                        }
                    } else {
                        Log.d("NFC", "Nenhum registro NDEF encontrado")
                    }
                } catch (e: Exception) {
                    Log.e("NFC", "Erro ao ler a tag NFC", e)
                }
            } ?: run {
                Log.d("NFC", "NDEF não suportado pela tag")
            }
        } ?: run {
            Log.d("NFC", "Tag não encontrada no Intent")
        }
    }
}
