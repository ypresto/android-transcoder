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

    private int outSizeSmall;
    private int outSizeLarge;

    public DefaultVideoStrategy(int firstSize, int secondSize) {
        this.outSizeSmall = Math.min(firstSize, secondSize);
        this.outSizeLarge = Math.max(firstSize, secondSize);
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
        copyInteger(inputFormat, format, MediaFormat.KEY_BIT_RATE);
        copyInteger(inputFormat, format, MediaFormat.KEY_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        return format;
    }

    private static void copyInteger(@NonNull MediaFormat input, @NonNull MediaFormat output, @NonNull String key) {
        if (input.containsKey(key)) {
            output.setInteger(key, input.getInteger(key));
        }
    }
}
