package com.zolarm.app.challenge

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraChallenge(modifier: Modifier = Modifier, onCaptured: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCaptured by rememberUpdatedState(onCaptured)

    var hasPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPerm = it }
    LaunchedEffect(Unit) { if (!hasPerm) launcher.launch(Manifest.permission.CAMERA) }

    if (!hasPerm) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("صلاحية الكاميرا مطلوبة", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("منح الصلاحية") }
        }
        return
    }

    var useFront by remember { mutableStateOf(true) }
    var isCapturing by remember { mutableStateOf(false) }
    var bound by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("وجّه وجهك للكاميرا") }
    var faceOk by remember { mutableStateOf(false) }
    var openOk by remember { mutableStateOf(false) }
    var moveOk by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val lock = remember { AtomicBoolean(false) }
    val lastCx = remember { floatArrayOf(Float.NaN) }
    val lastCy = remember { floatArrayOf(Float.NaN) }
    val moveAccum = remember { floatArrayOf(0f) }
    val moveFrames = remember { intArrayOf(0) }
    val openFrames = remember { intArrayOf(0) }

    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build()
        )
    }

    fun capture() {
        if (!lock.compareAndSet(false, true) || !bound) { lock.set(false); return }
        isCapturing = true
        status = "جاري الالتقاط…"
        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                image.close()
                currentOnCaptured()
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("ZolarmCam", "fail", exception)
                isCapturing = false
                lock.set(false)
                status = "فشل الالتقاط — أعد المحاولة"
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { detector.close() }
            runCatching { executor.shutdown() }
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    LaunchedEffect(lifecycleOwner, useFront) {
        bound = false
        lastCx[0] = Float.NaN; moveAccum[0] = 0f; moveFrames[0] = 0; openFrames[0] = 0
        faceOk = false; openOk = false; moveOk = false
        status = "وجّه وجهك للكاميرا"
        try {
            val provider = context.awaitCam()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null) { proxy.close(); return@setAnalyzer }
                val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                detector.process(img)
                    .addOnSuccessListener { faces ->
                        try {
                            if (faces.isEmpty()) {
                                lastCx[0] = Float.NaN; moveAccum[0] = 0f; moveFrames[0] = 0; openFrames[0] = 0
                                ContextCompat.getMainExecutor(context).execute {
                                    if (!isCapturing) {
                                        faceOk = false; openOk = false; moveOk = false
                                        status = "لم يُكتشف وجه — انظر للكاميرا"
                                    }
                                }
                                return@addOnSuccessListener
                            }
                            val face: Face = faces.maxBy { it.boundingBox.width() * it.boundingBox.height() }
                            val cx = face.boundingBox.exactCenterX()
                            val cy = face.boundingBox.exactCenterY()
                            if (!lastCx[0].isNaN()) {
                                val d = sqrt(abs(cx - lastCx[0]).let { it * it } + abs(cy - lastCy[0]).let { it * it })
                                if (d > 8f) { moveAccum[0] += d; moveFrames[0]++ }
                            }
                            lastCx[0] = cx; lastCy[0] = cy
                            val moving = moveAccum[0] > 45f && moveFrames[0] >= 4
                            val left = (face.leftEyeOpenProbability ?: 0f) > 0.5f
                            val right = (face.rightEyeOpenProbability ?: 0f) > 0.5f
                            val mouth = (face.smilingProbability ?: 0f) > 0.35f
                            val open = left || right || mouth
                            if (open) openFrames[0]++ else openFrames[0] = maxOf(0, openFrames[0] - 1)
                            val openStable = open && openFrames[0] >= 3
                            val msg = when {
                                openStable && moving -> "تم التحقق — جاري الالتقاط"
                                openStable -> "حرّك رأسك يميناً أو يساراً"
                                moving -> "افتح عيناً واحدة أو افتح فمك"
                                else -> "انظر للكاميرا وحرّك رأسك"
                            }
                            ContextCompat.getMainExecutor(context).execute {
                                if (isCapturing) return@execute
                                faceOk = true; openOk = openStable; moveOk = moving; status = msg
                                if (openStable && moving) capture()
                            }
                        } finally { proxy.close() }
                    }
                    .addOnFailureListener { proxy.close() }
            }
            val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, analysis)
            bound = true
        } catch (t: Throwable) {
            Log.e("ZolarmCam", "bind", t)
            status = "تعذّر تشغيل الكاميرا"
            bound = false
        }
    }

    val border = when {
        isCapturing -> MaterialTheme.colorScheme.primary
        faceOk && openOk && moveOk -> Color(0xFFB2FF59)
        faceOk -> Color(0xFFFFC107)
        else -> MaterialTheme.colorScheme.outline
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(28.dp))
                .border(3.dp, border, RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AndroidView({ previewView }, Modifier.matchParentSize())
            IconButton(
                onClick = { if (!isCapturing) useFront = !useFront },
                enabled = bound && !isCapturing,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) { Icon(Icons.Filled.Cameraswitch, "تبديل") }
        }
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(if (faceOk) "✅ ظهر الوجه" else "⬜ ظهر الوجه", color = if (faceOk) Color(0xFFB2FF59) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (moveOk) "✅ حركة الوجه" else "⬜ حركة الوجه", color = if (moveOk) Color(0xFFB2FF59) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (openOk) "✅ عين أو فم مفتوح" else "⬜ عين أو فم مفتوح", color = if (openOk) Color(0xFFB2FF59) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(status, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { if (!isCapturing) useFront = !useFront }, enabled = bound && !isCapturing) {
                Icon(Icons.Filled.Cameraswitch, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("تبديل")
            }
            Button(
                onClick = { if (faceOk && openOk && moveOk) capture() },
                enabled = bound && !isCapturing && faceOk && openOk && moveOk,
                shape = CircleShape, modifier = Modifier.size(84.dp)
            ) { Icon(Icons.Filled.CameraAlt, "التقاط", Modifier.size(34.dp)) }
        }
    }
}

private suspend fun Context.awaitCam(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
    val f = ProcessCameraProvider.getInstance(this)
    f.addListener({
        try { cont.resume(f.get()) } catch (t: Throwable) { cont.resumeWithException(t) }
    }, ContextCompat.getMainExecutor(this))
}
