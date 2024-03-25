package me.android.mycamera;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;

public class MockSocketServer {
    private static MockWebServer mockServer;

    private static boolean serverStarted = false;

    public static void startServer() throws IOException {
        mockServer = new MockWebServer();
        MockResponse mockResponse = new MockResponse()
                .withWebSocketUpgrade(new WebSocketListener() {
                    @Override
                    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                        Log.d("MockServer", "Connection Closed");
                        super.onClosed(webSocket, code, reason);
                    }

                    @Override
                    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                        super.onClosing(webSocket, code, reason);
                    }

                    @Override
                    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                        Log.d("MockServer", "Failed to start");
                        super.onFailure(webSocket, t, response);
                    }

                    @Override
                    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                        Log.d("MockServer", "Message Received");
                        super.onMessage(webSocket, text);
                    }

                    @Override
                    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                        Log.d("Received data", bytes.toString());
                        super.onMessage(webSocket, bytes);
                    }

                    @Override
                    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                        serverStarted = true;
                        Log.d("MockServer", "Started");
                        super.onOpen(webSocket, response);
                    }
                });
        mockServer.enqueue(mockResponse);
        mockServer.start();
    }

    public static String getUrl(){
        return "ws://"+mockServer.getHostName()+":"+mockServer.getPort();
    }

    public static void closeServer(){
        try {
            mockServer.close();
        }catch (Exception e){
            Log.d("Closing Server", "Error closing MockServer");
        }
    }

    public static boolean isServerStarted(){
        return serverStarted;
    }
}
