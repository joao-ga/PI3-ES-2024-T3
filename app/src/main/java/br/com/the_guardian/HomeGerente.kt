package br.com.the_guardian

import android.annotation.SuppressLint
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

    private lateinit var btnLiberarloc: AppCompatButton
    private lateinit var btnAbrirArm: AppCompatButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var btnSair: AppCompatButton
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_gerente) // Adicione o parêntese de fechamento aqui

        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        checkPermission()


        db.collection("Users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val nome = document.getString("name")
                    val isGerente = document.getBoolean("isManager") ?: false
                    if (isGerente) {
                        val apresentacao = findViewById<TextView>(R.id.apresentacao)
                        val mensagemBemVindo = getString(R.string.bem_vindo_gerente, nome)
                        apresentacao.text = mensagemBemVindo
                        break
                    }
                }
            }
            .addOnFailureListener { exception ->
                println("Erro ao obter documentos: $exception")
            }
        btnLiberarloc = findViewById(R.id.btnLiberarloc)
        btnAbrirArm = findViewById(R.id.btnAbrirArm)
        btnSair = findViewById(R.id.btnSair)

        btnLiberarloc.setOnClickListener{
            nextScreen(LiberarLocScreen::class.java)
        }


        btnAbrirArm.setOnClickListener {
            //nextScreen(AbrirArmScreen::class.java)
        }


        btnSair.setOnClickListener {
            auth = Firebase.auth
            auth.signOut()

            // limpar o estado de login no SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putBoolean(getString(R.string.logged_in_key), false)
            editor.apply()

            // redirecionar para a tela de login
            val intent = Intent(this, loginScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity()
        }

    }

    private fun nextScreen(screen: Class<*>) {
        val newScreen = Intent(this, screen)
        startActivity(newScreen)
    }

    private val CODE_PERMISSION_CAMERA = 123

    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.CAMERA),
                CODE_PERMISSION_CAMERA)
            return false
        }
        return true
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODE_PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida
            } else {
                Toast.makeText(this, "Permissão negada para utilizar a câmera!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
