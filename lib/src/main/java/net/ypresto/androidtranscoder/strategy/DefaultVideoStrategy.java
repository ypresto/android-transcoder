package net.ypresto.androidtranscoder.strategy;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import net.ypresto.androidtranscoder.utils.MediaFormatConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An {@link OutputStrategy} for video that converts it AVC with the given size.
 * The input and output aspect ratio must match.
 */
public class DefaultVideoStrategy implements OutputStrategy {

    private final static String MIME_TYPE = MediaFormatConstants.MIMETYPE_VIDEO_AVC;
    public final static int DEFAULT_I_FRAME_INTERVAL = 5;

    private int outSizeSmall;
    private int outSizeLarge;
    private Long bitRate;
    private int iFrameInterval;

    public DefaultVideoStrategy(int firstSize, int secondSize, @Nullable Long bitRate) {
        this(firstSize, secondSize, DEFAULT_I_FRAME_INTERVAL, bitRate);
    }

    public DefaultVideoStrategy(int firstSize, int secondSize, int iFrameInterval, @Nullable Long bitRate) {
        this.outSizeSmall = Math.min(firstSize, secondSize);
        this.outSizeLarge = Math.max(firstSize, secondSize);
        this.bitRate = bitRate;
        this.iFrameInterval = iFrameInterval;
    }

    @Nullable
    @Override
    public MediaFormat createOutputFormat(@NonNull MediaFormat inputFormat) throws OutputStrategyException {
        int inWidth = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int inHeight = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        int inSizeSmall = Math.min(inWidth, inHeight);
        int inSizeLarge = Math.max(inWidth, inHeight);
        if (inSizeSmall * outSizeLarge != inSizeLarge * outSizeSmall) {
            Exception cause = new IllegalArgumentException("Input and output ratio do not match. This is not supported yet.");
            throw OutputStrategyException.unavailable(cause);
        }
        if (inSizeSmall <= outSizeSmall && inputFormat.getString(MediaFormat.KEY_MIME).equals(MIME_TYPE)) {
            throw OutputStrategyException.alreadyCompressed("Input min: " + inSizeSmall + ", desired min: " + outSizeSmall);
        }
        int outWidth, outHeight;
        if (inWidth >= inHeight) {
            outWidth = outSizeLarge;
            outHeight = outSizeSmall;
        } else {
            outWidth = outSizeSmall;
            outHeight = outSizeLarge;
        }
        MediaFormat format = MediaFormat.createVideoFormat(
                MIME_TYPE, outWidth, outHeight);
        copyInteger(inputFormat, format, MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        if (bitRate != null) {
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate.intValue());
        } else {
            int frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
            format.setInteger(MediaFormat.KEY_BIT_RATE, (int) estimateBitRate(outWidth, outHeight, frameRate));
        }

        return format;
    }

    private static void copyInteger(@NonNull MediaFormat input, @NonNull MediaFormat output, @NonNull String key, @Nullable Integer fallback) {
        if (input.containsKey(key)) {
            output.setInteger(key, input.getInteger(key));
        } else if (fallback != null) {
            output.setInteger(key, fallback);
        }
    }

    // Depends on the codec, but for AVC this is a reasonable default ?
    // https://stackoverflow.com/a/5220554/4288782
    private static long estimateBitRate(int width, int height, int frameRate) {
        return (long) (0.07F * 2 * width * height * frameRate);
    }
}
