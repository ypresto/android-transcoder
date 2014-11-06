package net.ypresto.androidtranscoder.compat;

public class MediaCoderExtraConstants {
    // from API level >= 21;
    public static final String KEY_PROFILE = "profile"; // MediaCodecInfo.CodecProfileLevel
    // from (ANDROID ROOT)/media/libstagefright/ACodec.cpp
    public static final String KEY_LEVEL = "level"; // MediaCodecInfo.CodecProfileLevel
    private MediaCoderExtraConstants() {
    }
}
