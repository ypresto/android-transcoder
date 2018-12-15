package net.ypresto.androidtranscoder.strategy;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import net.ypresto.androidtranscoder.strategy.size.ExactSizer;
import net.ypresto.androidtranscoder.strategy.size.FractionSizer;
import net.ypresto.androidtranscoder.strategy.size.Size;
import net.ypresto.androidtranscoder.strategy.size.Sizer;
import net.ypresto.androidtranscoder.utils.Logger;
import net.ypresto.androidtranscoder.utils.MediaFormatConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An {@link OutputStrategy} for video that converts it AVC with the given size.
 * The input and output aspect ratio must match.
 */
public class DefaultVideoStrategy implements OutputStrategy {
    private final static String TAG = "DefaultVideoStrategy";
    private final static Logger LOG = new Logger(TAG);

    private final static String MIME_TYPE = MediaFormatConstants.MIMETYPE_VIDEO_AVC;
    public final static long BITRATE_UNKNOWN = Long.MIN_VALUE;
    public final static float DEFAULT_I_FRAME_INTERVAL = 3;
    public final static int DEFAULT_FRAME_RATE = 30;

    /**
     * Holds configuration values.
     */
    public static class Options {
        private Options() {}
        private Sizer sizer;
        private long targetBitRate;
        private int targetFrameRate;
        private float targetIFrameInterval;
    }

    public static Builder exact(int firstSize, int secondSize) {
        return new Builder(new ExactSizer(firstSize, secondSize));
    }

    public static Builder fraction(float fraction) {
        return new Builder(new FractionSizer(fraction));
    }

    public static class Builder {
        private Sizer sizer;
        private int targetFrameRate = DEFAULT_FRAME_RATE;
        private long targetBitRate = BITRATE_UNKNOWN;
        private float targetIFrameInterval = DEFAULT_I_FRAME_INTERVAL;

        private Builder(@NonNull Sizer sizer) {
            this.sizer = sizer;
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
            Options options = new Options();
            options.sizer = sizer;
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
        this(exact(firstSize, secondSize).frameRate(frameRate).iFrameInterval(iFrameInterval).bitRate(bitRate).options());
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
        LOG.i("Input width&height: " + inWidth + "x" + inHeight);
        Size inSize = new Size(inWidth, inHeight);
        Size outSize;
        try {
            outSize = options.sizer.getOutputSize(inSize);
        } catch (Exception e) {
            throw OutputStrategyException.unavailable(e);
        }
        int outWidth, outHeight;
        if (inWidth >= inHeight) {
            outWidth = outSize.getMajor();
            outHeight = outSize.getMinor();
        } else {
            outWidth = outSize.getMinor();
            outHeight = outSize.getMajor();
        }
        LOG.i("Output width&height: " + outWidth + "x" + outHeight);
        boolean sizeDone = inSize.getMinor() <= outSize.getMinor();

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
                    "Input minSize: " + inSize.getMinor() + ", desired minSize: " + outSize.getMinor() +
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
