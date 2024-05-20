package br.com.the_guardian

import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.ContentValues
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
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.nio.charset.Charset

class EncerrarLocScreen : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var db: FirebaseFirestore
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_encerrar_loc_screen)

        // Inicializa o NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        db = FirebaseFirestore.getInstance()  // Inicializa o Firestore

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
                val ndefMessages = rawMessages.mapNotNull { it as? NdefMessage }
                if (ndefMessages.isNotEmpty()) {
                    // Percorre todas as mensagens NDEF
                    for (message in ndefMessages) {
                        // Percorre todos os registros de cada mensagem NDEF
                        for (record in message.records) {
                            // Extrai os bytes do registro
                            val payload = record.payload

                            // Converte os bytes do payload para uma string
                            val uid = String(payload, Charset.defaultCharset())

                            // Chama o método endLocation com o UID
                            endLocation(uid)

                            // Limpa a tag NFC
                            clearNfcTag(intent)
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

    private fun clearNfcTag(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        tag?.let {
            val ndef = Ndef.get(it)
            if (ndef != null) {
                try {
                    // Cria um NdefMessage vazio
                    val emptyMessage = NdefMessage(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null))

                    // Habilita a conexão com a tag NFC
                    ndef.connect()
                    // Escreve o NdefMessage vazio na tag NFC
                    ndef.writeNdefMessage(emptyMessage)
                    // Fecha a conexão com a tag NFC
                    ndef.close()

                    Toast.makeText(this, "Dados da tag NFC limpos com sucesso", Toast.LENGTH_SHORT).show()

                    // Agendar a navegação para a tela HomeGerente após 5 segundos
                    handler.postDelayed({
                        val intent = Intent(this, HomeGerente::class.java)
                        startActivity(intent)
                        finish()
                    }, 5000) // 5000ms = 5s
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Erro ao limpar a tag NFC", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Erro desconhecido ao limpar a tag NFC", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "A tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
            }
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
                text.text = getString(R.string.locacao_encerrada)

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

    private fun endLocation(uid: String) {
        val user = uid
        db.collection("Locations").whereEqualTo("uid", user)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    document.reference.delete()
                        .addOnSuccessListener {
                            DataScreen.locacaoConfirmada =  false
                            Toast.makeText(this, "Locação encerrada!", Toast.LENGTH_SHORT).show()
                            Log.d("debugg", "Documento excluído com sucesso")
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Erro em cancelar pendência, tente de novo mais tarde!", Toast.LENGTH_SHORT).show()
                            Log.d(ContentValues.TAG, "Falha ao excluir o documento do usuário:", exception)
                        }
                } else {
                    Log.d(ContentValues.TAG, "Documento do usuário não encontrado")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "Falha ao obter o documento do usuário:", exception)
            }
    }


    // FALTA CALCULAR O PRECO DA DA LOCACAO


}
