package net.ypresto.androidtranscoder.strategy.size;

import java.util.ArrayList;

import androidx.annotation.NonNull;

/**
 * A {@link Resizer} that applies a chain of multiple resizers.
 * Of course order matters: the output of a resizer is the input of the next one.
 */
public class MultiResizer implements Resizer {

    private final ArrayList<Resizer> list = new ArrayList<>();

    // In this case we act as a pass through
    public MultiResizer() {}

    public MultiResizer(@NonNull Resizer... resizers) {
        for (Resizer resizer : resizers) {
            addResizer(resizer);
        }
    }

    public void addResizer(@NonNull Resizer resizer) {
        list.add(resizer);
    }

    @NonNull
    @Override
    public Size getOutputSize(@NonNull Size inputSize) throws Exception {
        Size size = inputSize;
        for (Resizer resizer : list) {
            size = resizer.getOutputSize(size);
        }
        return size;
    }
}
