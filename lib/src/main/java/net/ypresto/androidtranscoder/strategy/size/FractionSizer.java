package net.ypresto.androidtranscoder.strategy.size;

import androidx.annotation.NonNull;

/**
 * A {@link Sizer} that reduces the input size by the given fraction.
 * This ensures that output dimensions are not an odd number (refused by a few codecs).
 */
public class FractionSizer implements Sizer {

    private final float fraction;

    public FractionSizer(float fraction) {
        if (fraction <= 0 || fraction > 1) {
            throw new IllegalArgumentException("Fraction must be > 0 and <= 1");
        }
        this.fraction = fraction;
    }

    @NonNull
    @Override
    public Size getOutputSize(@NonNull Size inputSize) {
        int minor = (int) (fraction * inputSize.getMinor());
        int major = (int) (fraction * inputSize.getMajor());
        if (minor % 2 != 0) minor--;
        if (major % 2 != 0) major--;
        return new Size(minor, major);
    }
}
