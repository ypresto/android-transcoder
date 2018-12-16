package net.ypresto.androidtranscoder.strategy.size;

import androidx.annotation.NonNull;

/**
 * A general purpose interface that can be used (accepted as a parameter)
 * by video strategies such as {@link net.ypresto.androidtranscoder.strategy.DefaultVideoStrategy}
 * to compute the output size.
 */
public interface Resizer {

    /**
     * Parses the input size and returns the output.
     * This method should throw an exception if the input size is not processable.
     * @param inputSize the input video size
     * @return the output video size
     * @throws Exception if something is wrong with input size
     */
    @NonNull
    Size getOutputSize(@NonNull Size inputSize) throws Exception;
}
