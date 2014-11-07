package net.ypresto.androidtranscoder.engine;

import android.media.MediaFormat;

public interface TrackTranscoder {

    void setup();

    /**
     * Get actual MediaFormat which is used to write to muxer.
     * To determine you should call {@link #determineFormat()}.
     *
     * @return Actual output format determined by coder, or {@code null} if not yet determined.
     */
    MediaFormat getDeterminedFormat();

    /**
     * You should call this after {@link #determineFormat()} and before {@link #stepPipeline()}.
     * When all transcoder added their tracks then you may call {@link android.media.MediaMuxer#start()}.
     */
    void addTrackToMuxer();

    /**
     * Fill pipeline without writing to muxer until actual output format is determined.
     * You should not select any tracks on MediaExtractor to determine correctly.
     */
    void determineFormat();

    /**
     * Step pipeline if output is available in any step of it.
     * It assumes muxer has been started, so you should call muxer.start() first.
     *
     * @return true if data moved in pipeline.
     */
    boolean stepPipeline();

    /**
     * Get presentation time of last sample written to muxer.
     *
     * @return Presentation time in micro-second. Return value is undefined if finished writing.
     */
    long getWrittenPresentationTimeUs();

    boolean isFinished();

    void release();
}
