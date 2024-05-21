package br.com.the_guardian

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore

class ConfirmarUsuario : AppCompatActivity() {

    private lateinit var btnProsseguir: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String
    private lateinit var photoImageView: ImageView
    private lateinit var photoImageView2: ImageView
    private lateinit var btnVoltar: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_confirmar_usuario)

        btnProsseguir = findViewById(R.id.btnProsseguir)
        photoImageView = findViewById(R.id.photoImageView)
        photoImageView2 = findViewById(R.id.photoImageView2)
        db = FirebaseFirestore.getInstance()

        uid = intent.getStringExtra("uid").toString()
        Log.d("debug", uid)

        // Load the image from Firestore
        loadImageFromFirestore()
        loadImageFromFirestore2()

        // Inicialização do botão "Voltar" aqui, para garantir que ele esteja sempre visível
        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // botão para voltar para a home
            nextScreen(HomeGerente::class.java)
        }

        btnProsseguir.setOnClickListener {
            val dialog = AcaoArmario()
            dialog.show(supportFragmentManager, "dialog_lock_options")
        }
    }

    class AcaoArmario : BottomSheetDialogFragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

            val view = inflater.inflate(R.layout.dialog_lock_options, container, false)

            val btnOpenLock = view.findViewById<Button>(R.id.btnOpenLock)
            val btnCloseLock = view.findViewById<Button>(R.id.btnCloseLock)

            btnOpenLock.setOnClickListener {
                val dialog = FullScreenDialogFragment()
                dialog.show(parentFragmentManager, "dialog_loc_status")
            }

            btnCloseLock.setOnClickListener {
                activity?.let { nextScreen(it, EncerrarLocScreen::class.java) }
            }

            return view
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
                text.text = getString(R.string.arm_aberto)

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

    companion object {
        fun nextScreen(context: Context, screen: Class<*>) {
            val intent = Intent(context, screen)
            context.startActivity(intent)
        }
    }

    private fun loadImageFromFirestore() {
        db.collection("Locations")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.firstOrNull()
                    if (document != null) {
                        val photoPath = document.getString("photo")
                        if (!photoPath.isNullOrEmpty()) {
                            displayPhoto(photoPath, photoImageView)
                        } else {
                            Log.d("Firestore", "Photo path is null or empty")
                        }
                    } else {
                        Log.d("Firestore", "No document found with the specified uid")
                    }
                } else {
                    Log.d("Firestore", "No documents found")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("Firestore", "get failed with ", exception)
            }
    }

    private fun loadImageFromFirestore2() {
        db.collection("Locations")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.firstOrNull()
                    if (document != null) {
                        val photoPath = document.getString("photo2")
                        if (!photoPath.isNullOrEmpty()) {
                            displayPhoto(photoPath, photoImageView2)
                        } else {
                            Log.d("Firestore", "Photo path is null or empty")
                        }
                    } else {
                        Log.d("Firestore", "No document found with the specified uid")
                    }
                } else {
                    Log.d("Firestore", "No documents found")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("Firestore", "get failed with ", exception)
            }
    }


    private fun displayPhoto(photoUrl: String, imgView: ImageView) {
        Glide.with(this)
            .load(photoUrl)
            .into(imgView)
    }


    // função generica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val HomeGerente = Intent(this, screen)
        startActivity(HomeGerente)

    }
}