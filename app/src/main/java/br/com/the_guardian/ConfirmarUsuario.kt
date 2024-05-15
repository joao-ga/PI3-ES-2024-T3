package br.com.the_guardian

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ConfirmarUsuario : AppCompatActivity() {

    private lateinit var btnProsseguir: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_confirmar_usuario)

        btnProsseguir = findViewById(R.id.btnProsseguir)

        btnProsseguir.setOnClickListener{
            val dialog = AcaoArmario()
            dialog.show(supportFragmentManager, "dialog_lock_options")
        }
    }

    class AcaoArmario : BottomSheetDialogFragment(){

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

            val view = inflater.inflate(R.layout.dialog_lock_options, container, false)

            val btnOpenLock = view.findViewById<Button>(R.id.btnOpenLock)
            val btnCloseLock = view.findViewById<Button>(R.id.btnCloseLock)

            btnOpenLock.setOnClickListener{
                val dialog = FullScreenDialogFragment()
                dialog.show(parentFragmentManager, "dialog_loc_status")
            }

            btnCloseLock.setOnClickListener{
                btnCloseLock.setOnClickListener{
                    activity?.let { nextScreen(it, EncerrarLocScreen::class.java) }
                }
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
                text.text = "ARMÁRIO ABERTO"

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
}
