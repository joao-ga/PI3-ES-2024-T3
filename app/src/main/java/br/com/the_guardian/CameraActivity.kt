package br.com.the_guardian
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import br.com.the_guardian.databinding.ActivityCameraBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imgCaptureExecutor: ExecutorService
    private lateinit var db: FirebaseFirestore
    private var numberOfPersons = 1
    private var currentPerson = 1
    private lateinit var btnVoltar: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        numberOfPersons = intent.getIntExtra("NUMBER_PERSON", 1)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        // Inicialização do botão "Voltar" aqui, para garantir que ele esteja sempre visível
        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // botão para voltar para a home
            nextScreen(HomeGerente::class.java)
        }

        binding.btnTakePicture.setOnClickListener {
            takePictureAndProcess()
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            imageCapture = ImageCapture.Builder().build()
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("camera", "falha ao abrir a camera")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePictureAndProcess() {
        if (currentPerson <= numberOfPersons) {
            takePicture()
        }
    }

    private fun takePicture() {
        imageCapture?.let { imageCapture ->
            val fileName = "FOTO_JPEG_${System.currentTimeMillis()}_$currentPerson.jpg"
            val file = File(externalMediaDirs[0], fileName)

            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            imageCapture.takePicture(
                outputFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.d("debugg", "imagem salva no diretório: $outputFileResults")

                        if (currentPerson == 1) {
                            addPhoto(file, currentPerson)
                        } else {
                            addPhoto2(file, currentPerson)
                        }

                        currentPerson++

                        if (currentPerson > numberOfPersons) {
                            setResult(RESULT_OK)
                            val intent = Intent(this@CameraActivity, WriteNfc::class.java)
                            startActivity(intent)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            binding.root.context,
                            "Erro ao salvar foto",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("camera", "Erro ao gravar arquivo da foto: $exception")
                    }
                }
            )
        }
    }

    private fun addPhoto(photoFile: File, personIndex: Int) {
        val uid = QrCodeData.scannedData
        if (uid != null) {
            db.collection("Locations")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val documentId = document.id
                        val photoUrl = photoFile.absolutePath

                        db.collection("Locations")
                            .document(documentId)
                            .update("photo", photoUrl)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Foto atualizada com sucesso",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Erro ao atualizar foto: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("UpdatePhoto", "Erro ao atualizar foto", e)
                            }
                    } else {
                        Log.d("UpdatePhoto", "Documento do usuário não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("UpdatePhoto", "Falha ao obter o documento do usuário:", exception)
                }
        } else {
            Toast.makeText(this, "Erro: usuário não autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addPhoto2(photoFile: File, personIndex: Int) {
        val uid = QrCodeData.scannedData
        if (uid != null) {
            db.collection("Locations")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val documentId = document.id
                        val photoUrl = photoFile.absolutePath

                        db.collection("Locations")
                            .document(documentId)
                            .update("photo2", photoUrl)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Foto atualizada com sucesso",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Erro ao atualizar foto: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("UpdatePhoto", "Erro ao atualizar foto", e)
                            }
                    } else {
                        Log.d("UpdatePhoto", "Documento do usuário não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("UpdatePhoto", "Falha ao obter o documento do usuário:", exception)
                }
        } else {
            Toast.makeText(this, "Erro: usuário não autenticado", Toast.LENGTH_SHORT).show()
        }
    }


    // função generica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val HomeGerente = Intent(this, screen)
        startActivity(HomeGerente)

    }
}