package br.com.the_guardian

import android.annotation.SuppressLint
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.the_guardian.databinding.ActivityReadNfcBinding
import java.nio.charset.Charset

class ReadNfc : AppCompatActivity(), NfcAdapter.ReaderCallback {

    // variaveis de iniciação tardia
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var binding: ActivityReadNfcBinding
    private lateinit var btnVoltar: AppCompatButton

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

        // Inicialização do botão "Voltar" aqui, para garantir que ele esteja sempre visível
        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // botão para voltar para a home
            nextScreen(HomeGerente::class.java)
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

    // função para ler tags nfc
    @SuppressLint("MissingPermission")
    override fun onTagDiscovered(tag: Tag?) {
        // verifica se a tag nao é nula
        tag?.let {
            val ndef = Ndef.get(it)
            // verifica se a ndef nao é nula
            ndef?.let { ndef ->
                try {
                    // realiza a conecção com a tag nfc
                    ndef.connect()
                    // recebe a mensagem que veio da nfc
                    val ndefMessage = ndef.ndefMessage
                    // fecha a conecção
                    ndef.close()
                    val informacoes = ndefMessage.records
                    if (informacoes.isNotEmpty()) {
                        // ele transforma a mensagem para string e retira o dado importante
                        val firstRecord = informacoes[0]
                        val payload = firstRecord.payload
                        val text = String(payload, Charset.forName("UTF-8"))
                        val uid = text.substring(3)

                        runOnUiThread {
                            // Iniciar ConfirmarUsuario Activity com os dados da tag na thread princiapal
                            startActivity(Intent(this, ConfirmarUsuario::class.java).apply {
                                putExtra("uid", uid)
                            })
                        }
                        // logs de erro
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

    // função generica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val HomeGerente = Intent(this, screen)
        startActivity(HomeGerente)

    }
}
