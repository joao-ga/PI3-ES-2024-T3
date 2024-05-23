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

    // variaveis de inicialização tardia
    private var nfcAdapter: NfcAdapter? = null
    private var qrCodeContent: String? = null
    private lateinit var btnVoltar: AppCompatButton
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_nfc)
        db = FirebaseFirestore.getInstance()

        // obtem a classe nfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // verifica se o usuario tem NFC no dispositivo
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
        // Cria um PendingIntent que será usado para relançar a atividade atual quando uma tag NFC for detectada
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        // Define filtros de intenções para capturar a ação de descoberta de tag NFC
        val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        // Ativa a detecção de tags NFC enquanto esta atividade está em primeiro plano
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()
        // Desativa a detecção de tags NFC quando a atividade não está mais em primeiro plano
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("debug", "passei o super")
        // Obtém a ação do intent
        val action = intent.action
        Log.d("debug", "Ação do intent: $action")

        if (NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            Log.d("debug", "Ação encontrada")
            // Tenta obter a tag NFC a partir do intent
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                // Escreve dados na tag NFC e tenta recuperar informações do armário associado
                writeNfcTag(qrCodeContent.toString(), tag)

                getlocker(qrCodeContent)

            } else {
                // Exibe uma mensagem se nenhuma tag NFC for encontrada
                Toast.makeText(this, "Nenhuma tag NFC encontrada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeNfcTag(uid: String, tag: Tag) {
        try {
            // Obtém um objeto Ndef para a tag NFC
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                // Cria um registro NDEF com o UID fornecido
                val textRecord = NdefRecord.createTextRecord(null, uid)
                val ndefMessage = NdefMessage(arrayOf(textRecord))

                // Conecta à tag, escreve a mensagem NDEF e fecha a conexão
                ndef.connect()
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()

                // Exibe uma mensagem de sucesso
                Toast.makeText(this, "Locação realizada com sucesso", Toast.LENGTH_SHORT).show()
            } else {
                // Exibe uma mensagem se a tag NFC não suportar NDEF
                Toast.makeText(this, "A tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Exibe uma mensagem de erro se ocorrer uma exceção de E/S
            Toast.makeText(this, "Erro ao escrever na tag NFC: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            // Exibe uma mensagem de erro genérica para outras exceções
            Toast.makeText(this, "Erro ao escrever na tag NFC", Toast.LENGTH_SHORT).show()
        }
    }

    // Função para mudar para uma nova tela
    private fun nextScreen(screen: Class<*>) {
        val HomeGerente = Intent(this, screen)
        startActivity(HomeGerente)
    }

    private fun getlocker(uid: String?) {
        // Consulta a coleção "Locations" no Firestore para encontrar documentos onde o campo "uid" é igual ao UID fornecido
        db.collection("Locations")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Exibe uma mensagem se nenhum documento correspondente for encontrado
                    Toast.makeText(this, "Nenhum armário encontrado para este UID", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in documents) {
                        // Obtém o número do armário do documento
                        val locker = document.getString("locker")
                        if (locker != null) {
                            // Exibe uma mensagem com o número do armário alugado
                            Toast.makeText(this, "Armário alugado: $locker", Toast.LENGTH_SHORT).show()
                        } else {
                            // Exibe uma mensagem se os dados do armário não forem encontrados
                            Toast.makeText(this, "Dados do armário não encontrados", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Registra e exibe uma mensagem de erro se a consulta falhar
                Log.e("debug", "Erro ao recuperar dados: ", exception)
                Toast.makeText(this, "Erro ao recuperar dados do armário", Toast.LENGTH_SHORT).show()
            }
    }

}
