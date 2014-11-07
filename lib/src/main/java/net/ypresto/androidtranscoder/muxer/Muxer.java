package net.ypresto.androidtranscoder.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Abstraction interface to use non-Android muxer in same interface as {@link android.media.MediaMuxer}.
 */
public interface Muxer {
    void setOrientationHint(int degrees);

    void setLocation(float latitude, float longitude);

    void start();

    void stop();

    int addTrack(MediaFormat format);

    void writeSampleData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo);

    void release();
}
