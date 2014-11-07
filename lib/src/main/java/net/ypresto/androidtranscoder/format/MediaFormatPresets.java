package net.ypresto.androidtranscoder.format;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

// Refer for example: https://gist.github.com/wobbals/3990442
// Refer for preferred parameters: https://developer.apple.com/library/ios/documentation/networkinginternet/conceptual/streamingmediaguide/UsingHTTPLiveStreaming/UsingHTTPLiveStreaming.html#//apple_ref/doc/uid/TP40008332-CH102-SW8
// Refer for available keys: (ANDROID ROOT)/media/libstagefright/ACodec.cpp
public class MediaFormatPresets {

    private MediaFormatPresets() {
    }

    // preset similar to iOS SDK's AVAssetExportPreset960x540
    public static MediaFormat getExportPreset960x540() {
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", 960, 540);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 5400 * 1000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        return format;
    }
}
