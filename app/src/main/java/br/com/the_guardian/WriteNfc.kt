package br.com.the_guardian

import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class WriteNfc : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_write_nfc)

        // Inicializa o NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    override fun onResume() {
        super.onResume()

        // Verifica se o aparelho suporta a leitura de  uma NFC
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC não é suportado neste dispositivo", Toast.LENGTH_SHORT).show()
            return
        }

        //  Verifica se o NFC está habilitado no dispositivo
        if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Por favor, ative o NFC nas configurações do seu aparelho", Toast.LENGTH_SHORT)
                .show()
        } else {
            // lidar com a nfc - precisa receber e ser escrito na nfc os dados do armário alugado,
            // nome do usuário que alugou e foto(s) do(s) usuário(s)
        }


    }

}