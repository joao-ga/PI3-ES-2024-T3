package br.com.the_guardian

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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

        btnLiberarloc = findViewById(R.id.btnLiberarloc)
        btnAbrirArm = findViewById(R.id.btnAbrirArm)
        btnSair = findViewById(R.id.btnSair)


        btnLiberarloc.setOnClickListener {
            nextScreen(LiberarLocScreen::class.java)
        }

        btnAbrirArm.setOnClickListener {
            nextScreen(AbrirArmScreen::class.java)
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
}
