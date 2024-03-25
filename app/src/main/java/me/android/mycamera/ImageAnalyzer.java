package me.android.mycamera;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class ImageAnalyzer implements ImageAnalysis.Analyzer{
    private MediaCodec mediaCodec;
    private WebSocketClient webSocketClient;
    private WebSocketListener webSocketListener;
    private byte[] fallbackImage = null;
    private final ArrayBlockingQueue<byte[]> imageQueue = new ArrayBlockingQueue<>(20);
    private final ExecutorService imageDataExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService mediaCodecExecutor = Executors.newSingleThreadExecutor();
    boolean isCodecInitialized = false;
    private final Object queueLock = new Object();

    public ImageAnalyzer(){
        // Start Websocket Connection Inside
        ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("https://echo.websocket.org/");
                webSocketClient = new WebSocketClient(getWebSocketListener(), "https://echo.websocket.org/");
            }
        });

    }

    public void analyze(@NonNull ImageProxy image) {
        //Log.d("Analyze", String.valueOf(System.currentTimeMillis()));
        imageDataExecutor.execute( ()->
                processImageProxy(image));

    }

    @OptIn(markerClass = ExperimentalGetImage.class) private void processImageProxy(ImageProxy image){
        Image mImage = image.getImage();
        if (mImage != null) {
            try {
                // Convert the image buffer to a byte array
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] imageData = new byte[buffer.remaining()];
                buffer.get(imageData);
                enqueueImage(imageData);
                //System.out.println(Arrays.toString(imageData));

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                image.close();
            }
        }
    }

    private void enqueueImage(byte[] image) throws InterruptedException {
        //Log.d("ImageDataThread", String.valueOf(imageQueue.remainingCapacity()));
        synchronized (queueLock){
            if(image != null){
                imageQueue.offer(image);
                //Log.d("ImageDataThread", Arrays.toString(image));
            }
        }
        // start mediacodec
        if(!isCodecInitialized){
            Log.d("INIT", "Once");
            mediaCodecExecutor.execute(this::initMediaCodec);
            isCodecInitialized = true;
        }
    }

    private void initMediaCodec(){
        try {
            //start mediacodec
            if (mediaCodec == null) {
                mediaCodec = MediaCoder.getMediaCoder();
                mediaCodec.configure(MediaCoder.getFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mediaCodec.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
                        //System.out.println("input");
                        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(i);
                        inputBuffer.clear();
                        byte[] image;
                        synchronized (queueLock){
                            if(imageQueue.isEmpty()){
                                //Log.d("MediacodecThread", "Empty Queue");
                                image = fallbackImage;
                            }else{
                                //Log.d("MediacodecThread", "Not empty");
                                //Log.d("MediacodecThread", String.valueOf(System.currentTimeMillis()));
                                image = imageQueue.poll();
                                //Log.d("MediacodecThread", Arrays.toString(image));
                            }
                        }
                        if(image != null){
                            inputBuffer.put(image);
                            mediaCodec.queueInputBuffer(i, 0, image.length, 0, MediaCodec.CONFIGURE_FLAG_ENCODE);
                            fallbackImage = image;
                        }
                    }

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
                        //System.out.println("output");
                        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
                        MediaFormat bufferFormat = mediaCodec.getOutputFormat(i);
                        //Log.d("MediacodecThread", bufferFormat.toString());


                        //send bytebuffer using socket
                        webSocketClient.sendMessage(outputBuffer);
                        mediaCodec.releaseOutputBuffer(i, false);
                    }

                    @Override
                    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
                        Log.d("MediacodecThread", "Error");
                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
                        Log.d("MediacodecThread", "Output Changed");
                    }
                });
                mediaCodec.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private WebSocketListener getWebSocketListener(){
        webSocketListener = new WebSocketListener() {
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d("WebSocketClient", text);
                super.onMessage(webSocket, text);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                Log.d("WebSocketClient", bytes.toString());
                super.onMessage(webSocket, bytes);
            }

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                webSocketClient.setConnectionStatus(true);
                Log.d("WebSocketClient", "Connected"+response.toString());
                super.onOpen(webSocket, response);
            }
        };
        return webSocketListener;
    }




}
