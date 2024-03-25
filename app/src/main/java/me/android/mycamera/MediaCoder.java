package me.android.mycamera;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.DisplayMetrics;

import java.io.IOException;

public class MediaCoder {
    private static MediaCodec mediaCodec;

    public static MediaCodec getMediaCoder() throws IOException {
        if (mediaCodec == null){
            mediaCodec = MediaCodec.createEncoderByType("video/avc");

        }
        return  mediaCodec;
    }

    public static MediaFormat getFormat(){
        DisplayMetrics displayMetrics = MainActivity.getOwnDisplayMetric();

        int width = 720;
        int height = 480;
        System.out.println("width "+width+"  height "+height);
        int frameRate = 30; // Frames per second
        int bitRate = 700000;

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 7);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,350000);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

        return mediaFormat;
    }
}
