package dev.pan.snapper

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pan.snapper.data.TfLiteLandmarkClassifier
import dev.pan.snapper.domain.Classification
import dev.pan.snapper.presentation.LandmarkImageAnalyzer
import dev.pan.snapper.ui.theme.SnapperTheme
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    // type from CameraX that represents object of video feed that user records on camera
    private var recording: Recording? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                CAMERAX_PERMISSIONS,
                0
            )
        }

        setContent {
            SnapperTheme {
                // list of images we get to feed to AI
                var classifications by remember {
                    mutableStateOf(emptyList<Classification>())
                }

                val analyzer = remember {
                    LandmarkImageAnalyzer(
                        classifier = TfLiteLandmarkClassifier(
                            context = applicationContext
                        ),
                        onResults = {
                            classifications = it // adds value on change
                        }
                    )
                }

                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                        setImageAnalysisAnalyzer(
                            ContextCompat.getMainExecutor(applicationContext),
                            analyzer
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CameraPreview(
                        controller = controller,
                        modifier = Modifier
                            .fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        classifications.forEach{
                            Text(
                                text = it.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(8.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                    }

                }
            }
        }
    }

    // TODO: add swap between default camera and AI analyzer

//        enableEdgeToEdge()
//
//        if (!hasRequiredPermissions()) {
//            ActivityCompat.requestPermissions(
//                this,
//                CAMERAX_PERMISSIONS,
//                0
//            )
//        }
//
//        setContent {
//            SnapperTheme {
//                val scope = rememberCoroutineScope()
//                val scaffoldState = rememberBottomSheetScaffoldState()
//                val controller = remember {
//                    LifecycleCameraController(applicationContext).apply {
//                        setEnabledUseCases(
//                            CameraController.IMAGE_CAPTURE or
//                                    CameraController.VIDEO_CAPTURE
//                        )
//                    }
//                }
//                val viewModel = viewModel<MainViewModel>()
//                val bitmaps by viewModel.bitmaps.collectAsState()
//
//                BottomSheetScaffold(
//                    scaffoldState = scaffoldState,
//                    sheetPeekHeight = 0.dp,
//                    sheetContent = {
//                        PhotoBottomSheetContent(
//                            bitmaps = bitmaps,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                        )
//                    }
//                ) { innerPadding ->
//                    Box(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .padding(innerPadding)
//                    ) {
//                        CameraPreview(
//                            controller = controller,
//                            modifier = Modifier
//                                .fillMaxSize()
//                        )
//
//                        // button to switch camera front/back
//                        IconButton(
//                            onClick = {
//                                controller.cameraSelector =
//                                    if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
//                                        CameraSelector.DEFAULT_FRONT_CAMERA
//                                    } else CameraSelector.DEFAULT_BACK_CAMERA
//                            },
//                            modifier = Modifier
//                                .offset(16.dp, 16.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Cameraswitch,
//                                contentDescription = "Switch Camera"
//                            )
//                        }
//
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp)
//                                .align(Alignment.BottomCenter),
//                            horizontalArrangement = Arrangement.SpaceAround
//                        ) {
//                            IconButton(
//                                onClick = {
//                                    scope.launch {
//                                        scaffoldState.bottomSheetState.expand()
//                                    }
//                                }
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Photo,
//                                    contentDescription = "Open gallery"
//                                )
//                            }
//                            IconButton(
//                                onClick = {
//                                    takePhoto(
//                                        controller = controller,
//                                        onPhotoTaken = viewModel::onTakePhoto
//                                    )
//                                }
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.PhotoCamera,
//                                    contentDescription = "Take a photo"
//                                )
//                            }
//                            IconButton(
//                                onClick = {
//                                    recordVideo(controller)
//                                }
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Videocam,
//                                    contentDescription = "Record video"
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    private fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        if (!hasRequiredPermissions()) {
            return
        }

        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),

            // callback from camera library
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    // matrix used for any image transformations
                    val matrix = Matrix().apply {
                        // we can get info of taken photo that way
                        postRotate(image.imageInfo.rotationDegrees.toFloat())

                        // this can be used for front camera photos to make them mirrored in right way
//                        postScale(-1f, 1f)
                    }

                    // creating new bitmap and applying transformation on it (rotation from matrix)
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    // if photo taken successfully, we convert to Bitmap to process after in a way we need
                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)

                    Log.e("Camera", "Failed to take photo", exception)
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun recordVideo(controller: LifecycleCameraController) {
        // stop recording and save video if already in progress on button press
        if (recording != null) {
            recording?.stop()
            recording = null
            return
        }

        if (!hasRequiredPermissions()) {
            return
        }

        val outputFile = File(filesDir, "snapper_recorded_video.mp4")
        recording = controller.startRecording(
            FileOutputOptions.Builder(outputFile).build(),
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(applicationContext),
        ) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        recording?.close()
                        recording = null

                        Toast.makeText(
                            applicationContext,
                            "Video capture failed: ${event.error}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Video capture succeeded",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    }


    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
