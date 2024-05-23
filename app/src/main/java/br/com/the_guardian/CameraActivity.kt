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

    // Declaração das variáveis para os elementos da UI, Firebase e CameraX
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imgCaptureExecutor: ExecutorService
    private lateinit var db: FirebaseFirestore
    private var numberOfPersons = 1
    private var currentPerson = 1
    private lateinit var btnVoltar: AppCompatButton

    // Função onCreate, que inicializa a atividade
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializando o Firebase Firestore
        db = FirebaseFirestore.getInstance()

        // Obtendo o número de pessoas do Intent
        numberOfPersons = intent.getIntExtra("NUMBER_PERSON", 1)

        // Inicializando a CameraX
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        // Inicialização do botão "Voltar" para garantir que ele esteja sempre visível
        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            // Botão para voltar para a home
            nextScreen(HomeGerente::class.java)
        }

        // Configuração do botão para tirar a foto
        binding.btnTakePicture.setOnClickListener {
            takePictureAndProcess()
        }
    }

    // Função para iniciar a câmera
    private fun startCamera() {
        cameraProviderFuture.addListener({
            // Configurando ImageCapture e Preview
            imageCapture = ImageCapture.Builder().build()
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            try {
                // Vinculando a câmera ao ciclo de vida
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("camera", "Falha ao abrir a câmera")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Função para tirar a foto e processá-la
    private fun takePictureAndProcess() {
        if (currentPerson <= numberOfPersons) {
            takePicture()
        }
    }

    // Função para capturar a foto
    private fun takePicture() {
        imageCapture?.let { imageCapture ->
            // Criando o arquivo para salvar a imagem
            val fileName = "FOTO_JPEG_${System.currentTimeMillis()}_$currentPerson.jpg"
            val file = File(externalMediaDirs[0], fileName)

            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            // Capturando a imagem
            imageCapture.takePicture(
                outputFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        // Processando a imagem salva
                        if (currentPerson == 1) {
                            addPhoto(file, currentPerson)
                        } else {
                            addPhoto2(file, currentPerson)
                        }

                        currentPerson++

                        // Se todas as fotos foram tiradas, navegar para a próxima tela
                        if (currentPerson > numberOfPersons) {
                            setResult(RESULT_OK)
                            val intent = Intent(this@CameraActivity, WriteNfc::class.java)
                            startActivity(intent)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            // mensagem de erro
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

    // Função para adicionar a primeira foto ao Firestore
    private fun addPhoto(photoFile: File, personIndex: Int) {
        val uid = QrCodeData.scannedData
        if (uid != null) {
            // funcao que atualiza o documento, mudando o path da foto
            db.collection("Locations")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val documentId = document.id
                        val photoUrl = photoFile.absolutePath

                        db.collection("Locations")
                            // faz o update
                            .document(documentId)
                            .update("photo", photoUrl)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    // mensagem de sucesso
                                    this,
                                    "Primeira foto tirada com sucesso!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                // mensagem de erro
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

    // Função para adicionar a segunda foto ao Firestore
    private fun addPhoto2(photoFile: File, personIndex: Int) {
        val uid = QrCodeData.scannedData
        if (uid != null) {
            // funcao que atualiza o documento, mudando o path da foto
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
                            // faz o update
                            .update("photo2", photoUrl)
                            .addOnSuccessListener {
                                // mensagem de sucesso
                                Toast.makeText(
                                    this,
                                    "Segunda foto tirada com sucesso",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                // mensagem de erro
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

    // Função genérica para mudar de tela
    private fun nextScreen(screen: Class<*>) {
        val HomeGerente = Intent(this, screen)
        startActivity(HomeGerente)
    }
}