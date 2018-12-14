package net.ypresto.androidtranscoder.strategy;

import android.media.MediaFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Base class for video/audio format strategy.
 */
public interface OutputStrategy {

    /**
     * Create the output format for this track (either audio or video).
     * Implementors can:
     * - throw a {@link OutputStrategyException} if the whole transcoding should be aborted
     * - return {@param inputFormat} for remuxing this track as-is
     * - returning {@code null} for removing this track from output
     *
     * @param inputFormat the input format
     * @return the output format
     */
    @Nullable
    MediaFormat createOutputFormat(@NonNull MediaFormat inputFormat) throws OutputStrategyException;
}
