package br.com.the_guardian

import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import java.io.IOException

class EncerrarLocScreen : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_encerrar_loc_screen)

        // Inicializa o NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Verifica se o NFC está habilitado no dispositivo
        if (nfcAdapter != null && !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Por favor, ative o NFC nas configurações do seu aparelho", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        // Configura um PendingIntent para que esta atividade seja acionada quando uma tag NFC for detectada
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
                val ndefMessages = rawMessages.mapNotNull { it as? NdefMessage } // Flatten the list of messages
                if (ndefMessages.isNotEmpty()) {
                    // Percorre todas as mensagens NDEF
                    for (message in ndefMessages) {
                        // Percorre todos os registros de cada mensagem NDEF
                        for (record in message.records) {
                            // Extrai a tag NFC de cada registro
                            val tag = record.toByteArray()
                            // Limpar os dados na tag:
                            clearNfcTag(tag)

                            val dialog = ConfirmarUsuario.FullScreenDialogFragment()
                            dialog.show(supportFragmentManager, "dialog_loc_status")
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

    private fun clearNfcTag(tag: ByteArray) {
        try {
            // Converte o array de bytes em uma instância de Tag
            val nfcTag: Tag = tag as Tag
            // Obtém uma instância de Ndef para escrever na tag NFC
            val ndef = Ndef.get(nfcTag)
            if (ndef != null) {
                // Cria um NdefMessage vazio
                val emptyMessage = NdefMessage(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null))

                // Habilita a conexão com a tag NFC
                ndef.connect()
                // Escreve o NdefMessage vazio na tag NFC
                ndef.writeNdefMessage(emptyMessage)
                // Fecha a conexão com a tag NFC
                ndef.close()

                Toast.makeText(this, "Dados da tag NFC limpos com sucesso", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "A tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao limpar a tag NFC", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro desconhecido ao limpar a tag NFC", Toast.LENGTH_SHORT).show()
        }
    }

    class FullScreenDialogFragment : DialogFragment() {

        private val handler = Handler(Looper.getMainLooper())

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)

                // Infla o layout para o diálogo
                val inflater = requireActivity().layoutInflater
                val view = inflater.inflate(R.layout.dialog_loc_status, null)

                val text = view.findViewById<TextView>(R.id.text)
                text.text = "LOCAÇÃO ENCERRADA"

                // Adiciona o layout ao diálogo
                builder.setView(view)

                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }

        override fun onResume() {
            super.onResume()
            val window = dialog?.window
            val params = window?.attributes

            params?.width = WindowManager.LayoutParams.MATCH_PARENT
            params?.height = WindowManager.LayoutParams.MATCH_PARENT
            window?.attributes = params

            // Agendar a ação para ser executada após 5 segundos
            handler.postDelayed({
                val intent = Intent(activity, HomeGerente::class.java)
                startActivity(intent)
                dismiss()
            }, 5000) // 5000ms = 5s
        }

        override fun onPause() {
            super.onPause()

            // Cancelar qualquer ação agendada quando o diálogo for pausado
            handler.removeCallbacksAndMessages(null)
        }
    }

}
