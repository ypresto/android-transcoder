package net.ypresto.androidtranscoder.utils;

import android.media.MediaFormat;

import java.nio.ByteBuffer;

public class MediaFormatUtils {

    private MediaFormatUtils() {
    }

    public static String getMime(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    public static int getWidth(MediaFormat format) {
        return format.getInteger(MediaFormat.KEY_WIDTH);
    }

    public static int getHeight(MediaFormat format) {
        return format.getInteger(MediaFormat.KEY_HEIGHT);
    }

    public static ByteBuffer getSpsBuffer(MediaFormat format) {
        return format.getByteBuffer("csd-0").duplicate();
    }

    public static ByteBuffer getPpsBuffer(MediaFormat format) {
        return format.getByteBuffer("csd-1").duplicate();
    }
}
