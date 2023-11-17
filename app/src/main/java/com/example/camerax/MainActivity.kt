package com.example.camerax

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    var capture: ImageButton? = null
    var flash: ImageButton? = null
    var flipCamera: ImageButton? = null
    var previewView: PreviewView? = null
    var cameraFacing = CameraSelector.LENS_FACING_BACK
    private val activityResultLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            startCamera(cameraFacing)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.cameraPreview)
        capture = findViewById(R.id.btnCapture)
        flash = findViewById(R.id.flash)
        flipCamera = findViewById(R.id.flipCamera)

        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            activityResultLauncher.launch(android.Manifest.permission.CAMERA)
        }else{
            startCamera(cameraFacing)
        }

        flipCamera?.setOnClickListener{
            if(cameraFacing == CameraSelector.LENS_FACING_BACK){
                cameraFacing = CameraSelector.LENS_FACING_FRONT
            }else{
                cameraFacing = CameraSelector.LENS_FACING_BACK
            }
            startCamera(cameraFacing)
        }

    }

    private fun startCamera(cameraFacing: Int) {
        val aspectRatio = aspectRation(previewView!!.width, previewView!!.height)
        val listenableFuture = ProcessCameraProvider.getInstance(this)

        listenableFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = listenableFuture.get()

                // Libere recursos da câmera anterior, se houver
                cameraProvider.unbindAll()

                val preview = Preview.Builder()
                    .setTargetAspectRatio(aspectRatio)
                    .build()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(windowManager.defaultDisplay.rotation)
                    .build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraFacing)
                    .build()

                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                capture?.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        activityResultLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        takePicture(imageCapture)
                    }
                }

                flash?.setOnClickListener {
                    setFlash(camera)
                }

                preview.setSurfaceProvider(previewView?.surfaceProvider)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    private fun takePicture(imageCapture: ImageCapture){
        val file = File(getExternalFilesDir(null),"Teste.jpg")
        val outputFileOptionalExpectation = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptionalExpectation,Executors.newCachedThreadPool(),object :ImageCapture.OnImageSavedCallback{
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity,"Imagem salva",Toast.LENGTH_LONG).show()
                }
                startCamera(cameraFacing)
            }

            override fun onError(exception: ImageCaptureException) {
                runOnUiThread {
                Toast.makeText(this@MainActivity,"Imagem não foi salva",Toast.LENGTH_LONG).show()
                }
                startCamera(cameraFacing)
            }

        })
    }

    private fun setFlash(camera: Camera){
        if(camera.cameraInfo.hasFlashUnit()){
            if(camera.cameraInfo.torchState.value == 0){
                camera.cameraControl.enableTorch(true)
                flash?.setImageResource(R.drawable.flash_off_24)
            }else{
                camera.cameraControl.enableTorch(false)
                flash?.setImageResource(R.drawable.flash_on_24)
            }
        }else{
            runOnUiThread {
                Toast.makeText(this,"Flash não pode ser ligado",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun aspectRation(width: Int, height: Int): Int {
        if (width == height) {
            // Lidere com caso em que a largura e a altura são iguais (evite a divisão por zero)
            return AspectRatio.RATIO_4_3 // ou qualquer outra escolha padrão
        }

        val previewRatio = width.coerceAtLeast(height) / width.coerceAtMost(height)

        if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3
        }

        return AspectRatio.RATIO_16_9
    }
}