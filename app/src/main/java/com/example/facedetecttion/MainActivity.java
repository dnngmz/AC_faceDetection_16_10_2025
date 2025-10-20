package com.example.facedetecttion;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private boolean detectar = false;

    private Button btnDetectar;
    private TextView tvEmocion;
    private FrameLayout emojiContainer;

    private List<String> emojiSonrientes = Arrays.asList("😄", "😂", "🥳", "😁");
    private List<String> emojiSerios = Arrays.asList("😐", "😑", "😶", "🤨");
    private List<String> emojiNeutrales = Arrays.asList("🙂", "😌", "😏");

    private EmocionReceiver emocionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        btnDetectar = findViewById(R.id.btnDetectar);
        tvEmocion = findViewById(R.id.tvEmocion);
        emojiContainer = findViewById(R.id.emojiContainer);

        cameraExecutor = Executors.newSingleThreadExecutor();

        emocionReceiver = new EmocionReceiver(tvEmocion, emojiContainer, emojiSonrientes, emojiSerios, emojiNeutrales);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        btnDetectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectar = !detectar;

                if (detectar) {
                    previewView.setVisibility(View.VISIBLE);
                    startCamera();
                    btnDetectar.setText("Stop Scan");
                } else {
                    stopCamera();
                    previewView.setVisibility(View.GONE);
                    btnDetectar.setText("Start Scan");
                }
            }
        });
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();

                    Preview preview = new Preview.Builder().build();
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build();

                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

                    imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                        @Override
                        public void analyze(@NonNull ImageProxy imageProxy) {
                            if (detectar) {
                                analizarImagen(imageProxy);
                            } else {
                                imageProxy.close();
                            }
                        }
                    });

                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageAnalysis);

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCamera() {
        if (cameraProvider != null) cameraProvider.unbindAll();
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analizarImagen(final ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<List<Face>>() {
                        @Override
                        public void onSuccess(List<Face> faces) {
                            procesarRostros(faces);
                        }
                    })

                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                        }
                    })

                    .addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<List<Face>>() {
                        @Override
                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<List<Face>> task) {
                            imageProxy.close();
                        }
                    });

        } else {
            imageProxy.close();
        }
    }

    private void procesarRostros(List<Face> faces) {
        if (faces.isEmpty()) return;

        for (Face face : faces) {
            Float probSonrisa = face.getSmilingProbability();
            if (probSonrisa != null) {
                String emocion;
                if (probSonrisa > 0.7f) {
                    emocion = "Sonriente 😄";
                } else if (probSonrisa < 0.3f) {
                    emocion = "Serio 😐";
                } else {
                    emocion = "Neutral 🙂";
                }

                Intent intent = new Intent("com.example.emociondetector.EMOCION_DETECTADA");
                intent.setPackage(getPackageName());
                intent.putExtra("emocion", emocion);
                sendBroadcast(intent);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(emocionReceiver, new IntentFilter("com.example.emociondetector.EMOCION_DETECTADA"), RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(emocionReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        stopCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
