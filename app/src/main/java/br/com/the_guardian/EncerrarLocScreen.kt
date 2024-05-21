package br.com.the_guardian

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.nio.charset.Charset

class EncerrarLocScreen : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var db: FirebaseFirestore
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var btnVoltar: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    @SuppressLint("MissingPermission")
    override fun onTagDiscovered(tag: Tag?) {
        tag?.let {
            val ndef = Ndef.get(it)
            ndef?.let { ndef ->
                try {
                    ndef.connect()
                    val ndefMessage = ndef.ndefMessage
                    Log.d("debug", ndefMessage.toString())

                    val informacoes = ndefMessage.records
                    if (informacoes.isNotEmpty()) {
                        val firstRecord = informacoes[0]
                        val payload = firstRecord.payload
                        val text = String(payload, Charset.forName("UTF-8"))
                        val uid = text.substring(3)
                        Log.d("NFC", "Tag detectada: $uid")

                        runOnUiThread {
                            // Chama o método endLocation com o UID
                            endLocation(uid)
                        }

                        // Limpa a tag NFC
                        clearNfcTag(ndef)

                        Toast.makeText(this, "Dados da tag NFC limpos com sucesso", Toast.LENGTH_SHORT).show()

                    } else {
                        Log.d("NFC", "Nenhum registro NDEF encontrado")
                    }
                    ndef.close()
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

    private fun clearNfcTag(ndef: Ndef) {
        try {
            // Cria um NdefMessage vazio
            val emptyMessage = NdefMessage(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null))

            // Escreve o NdefMessage vazio na tag NFC
            ndef.writeNdefMessage(emptyMessage)

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
    }

    private fun endLocation(uid: String) {
        db.collection("Locations").whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    document.reference.delete()
                        .addOnSuccessListener {
                            DataScreen.locacaoConfirmada = false
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

    // função generica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val HomeGerente = Intent(this, screen)
        startActivity(HomeGerente)

    }
}
