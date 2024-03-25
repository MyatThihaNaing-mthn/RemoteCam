package me.android.mycamera;

import android.util.Log;

import java.nio.ByteBuffer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient {
    private WebSocket webSocket;

    private boolean isConnected;
    private final Request request;
    private final OkHttpClient okHttpClient;

    public WebSocketClient(WebSocketListener webSocketListener, String socketUrl){
        request = new Request.Builder().url(socketUrl).build();
        this.okHttpClient = new OkHttpClient();
        this.webSocket = okHttpClient.newWebSocket(request, webSocketListener);
    }

    public void setWebSocket(WebSocket webSocket){
        this.webSocket = webSocket;
    }


    public void sendMessage(ByteBuffer byteBuffer){
        //Log.d("Sending Message", String.valueOf(isConnected));
       if (byteBuffer != null && isConnected)
       {
           byte[] byteArray = new byte[byteBuffer.remaining()];
           byteBuffer.get(byteArray);
           ByteString byteString = ByteString.of(byteArray, 0, byteArray.length);
           try {
               Log.d("WebSocket", byteString.toString());
               webSocket.send(byteString);
           }
           catch (Exception e){
               Log.d("Websocket", "Error sending message");
           }
       }
    }

    public void closeConnection(){
        webSocket.close(1000, "User asked to close the connection");
    }

    // To call from onOpen fun of websocket listener
    public void setConnectionStatus(boolean connectionStatus){
        this.isConnected = connectionStatus;
    }


}
