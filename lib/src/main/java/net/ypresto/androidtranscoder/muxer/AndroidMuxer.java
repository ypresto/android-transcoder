package net.ypresto.androidtranscoder.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AndroidMuxer implements Muxer {
    private static final String TAG = "AndroidMuxer";
    private MediaMuxer mMediaMuxer;

    public AndroidMuxer(String path, int format) throws IOException {
        mMediaMuxer = new MediaMuxer(path, format);
    }

    @Override
    public void setOrientationHint(int degrees) {
        mMediaMuxer.setOrientationHint(degrees);
    }

    @Override
    public void setLocation(float latitude, float longitude) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mMediaMuxer.setLocation(latitude, longitude);
        } else {
            Log.w(TAG, "setLocation: skipped because it is NOT supported by Android API level of this device.");
        }
    }

    @Override
    public void start() {
        mMediaMuxer.start();
    }

    @Override
    public void stop() {
        mMediaMuxer.stop();
    }

    @Override
    public int addTrack(MediaFormat format) {
        return mMediaMuxer.addTrack(format);
    }

    @Override
    public void writeSampleData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
    }

    @Override
    public void release() {
        mMediaMuxer.release();
    }
}
