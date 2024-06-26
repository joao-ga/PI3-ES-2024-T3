package br.com.the_guardian

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class HomeGerente : AppCompatActivity() {

    // Declaração dos componentes da UI e outros objetos necessários
    private lateinit var btnLiberarloc: AppCompatButton
    private lateinit var btnAbrirArm: AppCompatButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var btnSair: AppCompatButton
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Adicione o parêntese de fechamento aqui
        setContentView(R.layout.activity_home_gerente)

        // Inicializa SharedPreferences, FirebaseAuth e FirebaseFirestore
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Verifica permissões necessárias
        checkPermission()
        // Obtém e exibe o nome do gerente
        getManagerName()

        // Inicializa os botões da interface de usuário
        btnLiberarloc = findViewById(R.id.btnLiberarloc)
        btnAbrirArm = findViewById(R.id.btnAbrirArm)
        btnSair = findViewById(R.id.btnSair)

        // Configura os listeners dos botões
        btnLiberarloc.setOnClickListener{
            // Muda para a tela de liberar localização
            nextScreen(LiberarLocScreen::class.java)
        }

        btnAbrirArm.setOnClickListener {
            // Muda para a tela de leitura de NFC
            nextScreen(ReadNfc::class.java)
        }

        btnSair.setOnClickListener {
            auth = Firebase.auth
            // Faz logout do usuário
            auth.signOut()

            // Limpa o estado de login no SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putBoolean(getString(R.string.logged_in_key), false)
            editor.apply()

            // Redireciona para a tela de login
            val intent = Intent(this, loginScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            // Encerra todas as atividades associadas
            finishAffinity()
        }
    }

    // Função para mudar para outra tela
    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }

    private val CODE_PERMISSION_CAMERA = 123

    // Função para verificar a permissão da câmera
    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Solicita a permissão se não estiver concedida
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.CAMERA),
                CODE_PERMISSION_CAMERA)
            return false
        }
        return true
    }

    // Função de callback para o resultado da solicitação de permissões
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODE_PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida
            } else {
                // Permissão negada
                Toast.makeText(this, "Permissão negada para utilizar a câmera!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função para obter o nome do gerente e exibir na tela
    fun getManagerName() {
        // Obtém o ID do usuário atual e pega o nome do usuario
        val currentUser = auth.currentUser?.uid
        db.collection("Users").
            whereEqualTo("uid", currentUser)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val nome = document.getString("name")
                    val isGerente = document.getBoolean("isManager") ?: false
                    if (isGerente) {
                        // Atualiza o TextView com a mensagem de boas-vindas
                        val apresentacao = (this as Activity).findViewById<TextView>(R.id.apresentacao)
                        val mensagemBemVindo = this.getString(R.string.bem_vindo_gerente, nome)
                        apresentacao.text = mensagemBemVindo
                        break
                    }
                }
            }
            .addOnFailureListener { exception ->
                println("Erro ao obter documentos: $exception") // Log de erro
            }
    }

}
