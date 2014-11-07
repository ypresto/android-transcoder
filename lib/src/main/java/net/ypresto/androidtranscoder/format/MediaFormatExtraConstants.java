package net.ypresto.androidtranscoder.format;

public class MediaFormatExtraConstants {
    // from API level >= 21, but might be usable in older APIs as native code implementation exists.
    public static final String KEY_PROFILE = "profile"; // MediaCodecInfo.CodecProfileLevel
    // from (ANDROID ROOT)/media/libstagefright/ACodec.cpp
    public static final String KEY_LEVEL = "level"; // MediaCodecInfo.CodecProfileLevel
    private MediaFormatExtraConstants() {
    }
}
