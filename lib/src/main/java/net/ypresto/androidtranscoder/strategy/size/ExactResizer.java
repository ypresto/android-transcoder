package net.ypresto.androidtranscoder.strategy.size;

import androidx.annotation.NonNull;

/**
 * A {@link Resizer} that returns the exact dimensions that were passed as input.
 * Throws if the aspect ratio does not match.
 */
public class ExactResizer implements Resizer {

    private final Size output;

    public ExactResizer(int first, int second) {
        output = new Size(first, second);
    }

    @NonNull
    @Override
    public Size getOutputSize(@NonNull Size inputSize) {
        if (inputSize.getMinor() * output.getMajor() != inputSize.getMajor() * output.getMinor()) {
            throw new IllegalStateException("Input and output ratio do not match.");
        }
        return output;
    }
}
