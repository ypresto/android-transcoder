package net.ypresto.androidtranscoder.strategy;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import net.ypresto.androidtranscoder.utils.MediaFormatConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An {@link OutputStrategy} for video that converts it AVC with the given size.
 * The input and output aspect ratio must match.
 */
public class DefaultVideoStrategy implements OutputStrategy {

    private final static String MIME_TYPE = MediaFormatConstants.MIMETYPE_VIDEO_AVC;
    public final static long BITRATE_UNKNOWN = Long.MIN_VALUE;
    public final static float DEFAULT_I_FRAME_INTERVAL = 3;

    /**
     * Holds configuration values.
     */
    public static class Options {
        private Options() {}
        private int targetSizeSmall;
        private int targetSizeLarge;
        private long targetBitRate;
        private int targetFrameRate;
        private float targetIFrameInterval;
    }

    public static Builder builder(int firstSize, int secondSize) {
        return new Builder(firstSize, secondSize);
    }

    public static class Builder {
        private int targetSizeSmall;
        private int targetSizeLarge;
        private Integer targetFrameRate;
        private long targetBitRate = BITRATE_UNKNOWN;
        private float targetIFrameInterval = DEFAULT_I_FRAME_INTERVAL;

        public Builder(int firstSize, int secondSize) {
            targetSizeSmall = Math.min(firstSize, secondSize);
            targetSizeLarge = Math.max(firstSize, secondSize);
        }

        /**
         * The desired bit rate. Can optionally be {@link #BITRATE_UNKNOWN},
         * in which case the strategy will try to estimate the bitrate.
         * @param bitRate desired bit rate (bits per second)
         * @return this for chaining
         */
        public Builder bitRate(long bitRate) {
            targetBitRate = bitRate;
            return this;
        }

        /**
         * The desired frame rate. It will never be bigger than
         * the input frame rate, if that information is available.
         * @param frameRate desired frame rate (frames per second)
         * @return this for chaining
         */
        public Builder frameRate(int frameRate) {
            targetFrameRate = frameRate;
            return this;
        }

        /**
         * The interval between I-frames in seconds.
         * @param iFrameInterval desired i-frame interval
         * @return this for chaining
         */
        public Builder iFrameInterval(float iFrameInterval) {
            targetIFrameInterval = iFrameInterval;
            return this;
        }

        public Options options() {
            if (targetFrameRate == null) {
                throw new IllegalArgumentException("Frame rate can not be null.");
            }
            Options options = new Options();
            options.targetSizeSmall = targetSizeSmall;
            options.targetSizeLarge = targetSizeLarge;
            options.targetFrameRate = targetFrameRate;
            options.targetBitRate = targetBitRate;
            options.targetIFrameInterval = targetIFrameInterval;
            return options;
        }

        public DefaultVideoStrategy build() {
            return new DefaultVideoStrategy(options());
        }
    }

    private final Options options;

    public DefaultVideoStrategy(int firstSize, int secondSize, int frameRate) {
        this(firstSize, secondSize, frameRate, DEFAULT_I_FRAME_INTERVAL, BITRATE_UNKNOWN);
    }

    public DefaultVideoStrategy(int firstSize, int secondSize, int frameRate, long bitRate) {
        this(firstSize, secondSize, frameRate, DEFAULT_I_FRAME_INTERVAL, bitRate);
    }

    public DefaultVideoStrategy(int firstSize, int secondSize, int frameRate, float iFrameInterval, long bitRate) {
        this(builder(firstSize, secondSize).frameRate(frameRate).iFrameInterval(iFrameInterval).bitRate(bitRate).options());
    }

    public DefaultVideoStrategy(@NonNull Options options) {
        this.options = options;
    }

    @Nullable
    @Override
    public MediaFormat createOutputFormat(@NonNull MediaFormat inputFormat) throws OutputStrategyException {
        boolean typeDone = inputFormat.getString(MediaFormat.KEY_MIME).equals(MIME_TYPE);

        // Compute output size.
        int inWidth = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int inHeight = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        int inSizeSmall = Math.min(inWidth, inHeight);
        int inSizeLarge = Math.max(inWidth, inHeight);
        if (inSizeSmall * options.targetSizeLarge != inSizeLarge * options.targetSizeSmall) {
            Exception cause = new IllegalArgumentException("Input and output ratio do not match. This is not supported yet.");
            throw OutputStrategyException.unavailable(cause);
        }
        int outWidth, outHeight;
        if (inWidth >= inHeight) {
            outWidth = options.targetSizeLarge;
            outHeight = options.targetSizeSmall;
        } else {
            outWidth = options.targetSizeSmall;
            outHeight = options.targetSizeLarge;
        }
        boolean sizeDone = inSizeSmall <= options.targetSizeSmall;

        // Compute output frame rate. It can't be bigger than input frame rate.
        int inputFrameRate, outFrameRate;
        if (inputFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            inputFrameRate = inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            outFrameRate = Math.min(inputFrameRate, options.targetFrameRate);
        } else {
            inputFrameRate = -1;
            outFrameRate = options.targetFrameRate;
        }
        boolean frameRateDone = inputFrameRate <= outFrameRate;

        // Compute i frame.
        int inputIFrameInterval = -1;
        if (inputFormat.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) {
            inputIFrameInterval = inputFormat.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL);
        }
        boolean frameIntervalDone = inputIFrameInterval >= options.targetIFrameInterval;

        // See if we should go on.
        if (typeDone && sizeDone && frameRateDone && frameIntervalDone) {
            throw OutputStrategyException.alreadyCompressed(
                    "Input minSize: " + inSizeSmall + ", desired minSize: " + options.targetSizeSmall +
                    "\nInput frameRate: " + inputFrameRate + ", desired frameRate: " + outFrameRate +
                    "\nInput iFrameInterval: " + inputIFrameInterval + ", desired iFrameInterval: " + options.targetIFrameInterval);
        }

        // Create the actual format.
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, outWidth, outHeight);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, outFrameRate);
        if (Build.VERSION.SDK_INT >= 25) {
            format.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, options.targetIFrameInterval);
        } else {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, (int) Math.ceil(options.targetIFrameInterval));
        }
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        int outBitRate = (int) (options.targetBitRate == BITRATE_UNKNOWN ?
                estimateBitRate(outWidth, outHeight, outFrameRate) : options.targetBitRate);
        format.setInteger(MediaFormat.KEY_BIT_RATE, outBitRate);
        return format;
    }

    // Depends on the codec, but for AVC this is a reasonable default ?
    // https://stackoverflow.com/a/5220554/4288782
    private static long estimateBitRate(int width, int height, int frameRate) {
        return (long) (0.07F * 2 * width * height * frameRate);
    }

    private static void copyInteger(@NonNull MediaFormat input, @NonNull MediaFormat output,
                                    @NonNull String key, @Nullable Integer fallback) {
        if (input.containsKey(key)) {
            output.setInteger(key, input.getInteger(key));
        } else if (fallback != null) {
            output.setInteger(key, fallback);
        }
    }
}
