package me.android.mycamera;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final int  REQUEST_CODE_PERMISSION = 1111;
    private static WindowManager windowManager = null;
    ImageAnalysis imageAnalysis = null;

    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private PreviewView mPreview;
    private final String[] requiredPermissions = new String[]{
            "android.permission.CAMERA",
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        windowManager = getWindowManager();

        mPreview = findViewById(R.id.camera_view);
        if(allPermissionGranted()){
            startCamera();
        }else {
            askPermissions();
        }
    }

    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    //Initialize image analysis
                    imageAnalysis = new ImageAnalysis.Builder().setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build();
                    imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalyzer());

                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider){
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(mPreview.getSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

    }

    @Override
    public void onDestroy(){
        MockSocketServer.closeServer();
        super.onDestroy();
    }

    private boolean allPermissionGranted(){
        for(String permission: requiredPermissions){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSION){
            if(allPermissionGranted()){
                startCamera();
            }else{
                Toast.makeText(this, "You need to give permissions", Toast.LENGTH_SHORT).show();
                askPermissions();
            }
        }
    }

    private void askPermissions(){
        ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSION);
    }

    public static DisplayMetrics getOwnDisplayMetric(){
        // Should have return default metrics if window manager is null
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if(windowManager!=null){
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return displayMetrics;
    }

}