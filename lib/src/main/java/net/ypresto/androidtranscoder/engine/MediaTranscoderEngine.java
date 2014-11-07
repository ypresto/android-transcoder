package net.ypresto.androidtranscoder.engine;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;

import net.ypresto.androidtranscoder.utils.MediaExtractorUtils;

import java.io.FileDescriptor;
import java.io.IOException;

// TODO: treat encrypted data
public class MediaTranscoderEngine {
    private static final String TAG = "MediaTranscoderEngine";
    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 10;

    static {
    }

    private FileDescriptor mInputFileDescriptor;
    private TrackTranscoder mVideoTrackTranscoder;
    private TrackTranscoder mAudioTrackTranscoder;
    private MediaExtractor mExtractor;
    private MediaMuxer mMuxer;

    public MediaTranscoderEngine() {
    }

    public void setDataSource(FileDescriptor fileDescriptor) {
        mInputFileDescriptor = fileDescriptor;
    }

    /**
     * Run transcoding. Blocks current thread.
     * @throws IOException when extractor or muxer cannot open file.
     */
    public void transcode(String outputPath, MediaFormat outputFormat) throws IOException {
        if (outputPath == null) {
            throw new NullPointerException("Output path cannot be null.");
        }
        if (mInputFileDescriptor == null) {
            throw new IllegalStateException("Data source is not set.");
        }
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mInputFileDescriptor);
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            setupTrackTranscoders(outputFormat);
            mMuxer.start();
            runPipelines();
            mMuxer.stop();
        } finally {
            if (mVideoTrackTranscoder != null) {
                mVideoTrackTranscoder.release();
                mVideoTrackTranscoder = null;
            }
            if (mAudioTrackTranscoder != null) {
                mAudioTrackTranscoder.release();
                mAudioTrackTranscoder = null;
            }
            if (mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }
            if (mMuxer != null) {
                mMuxer.release();
                mMuxer = null;
            }
        }
    }

    private void setupProgressCalculation() throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(mInputFileDescriptor);
        // TODO
    }

    private void setupTrackTranscoders(MediaFormat outputFormat) {
        MediaExtractorUtils.TrackResult trackResult = MediaExtractorUtils.getFirstVideoAndAudioTrack(mExtractor);
        mVideoTrackTranscoder = new VideoTrackTranscoder(mExtractor, trackResult.mVideoTrackIndex, outputFormat, mMuxer);
        mVideoTrackTranscoder.setup();
        mAudioTrackTranscoder = new PassThroughTrackTranscoder(mExtractor, trackResult.mAudioTrackIndex, mMuxer);
        mAudioTrackTranscoder.setup();
        mVideoTrackTranscoder.determineFormat();
        mAudioTrackTranscoder.determineFormat();
        mVideoTrackTranscoder.addTrackToMuxer();
        mAudioTrackTranscoder.addTrackToMuxer();
        mExtractor.selectTrack(trackResult.mVideoTrackIndex);
        mExtractor.selectTrack(trackResult.mAudioTrackIndex);
    }

    private void runPipelines() {
        int stepCount = 0;
        while (!(mVideoTrackTranscoder.isFinished() && mAudioTrackTranscoder.isFinished())) {
            boolean stepped = mVideoTrackTranscoder.stepPipeline()
                    || mAudioTrackTranscoder.stepPipeline();
            if (true) continue;
            if (!stepped) {
                try {
                    Log.v(TAG, "Sleeping " + SLEEP_TO_WAIT_TRACK_TRANSCODERS + "msec, " + stepCount + " steps run after last sleep.");
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
                } catch (InterruptedException e) {
                    // nothing to do
                }
                stepCount = 0;
                continue;
            }
            stepCount++;
        }
    }
}
