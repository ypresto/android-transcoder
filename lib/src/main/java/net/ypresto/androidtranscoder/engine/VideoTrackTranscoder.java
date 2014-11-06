package net.ypresto.androidtranscoder.engine;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.nio.ByteBuffer;

// Refer: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
public class VideoTrackTranscoder implements TrackTranscoder {
    private static final String TAG = "VideoTrackTranscoder";
    private static final int DRAIN_STATE_NONE = 0;
    private static final int DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1;
    private static final int DRAIN_STATE_CONSUMED = 2;

    private final MediaExtractor mExtractor;
    private final int mTrackIndex;
    private final MediaFormat mOutputFormat;
    private final MediaMuxer mMuxer;
    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private boolean mWritingToMuxerStarted;
    private MediaCodec mDecoder;
    private MediaCodec mEncoder;
    private ByteBuffer[] mDecoderInputBuffers;
    private ByteBuffer[] mEncoderOutputBuffers;
    private MediaFormat mActualOutputFormat;
    private int mMuxerTrackIndex = -1;
    private OutputSurface mDecoderOutputSurfaceWrapper;
    private InputSurface mEncoderInputSurfaceWrapper;
    private boolean mIsExtractorEOS;
    private boolean mIsDecoderEOS;
    private boolean mIsEncoderEOS;
    private boolean mDecoderStarted;
    private boolean mEncoderStarted;

    public VideoTrackTranscoder(MediaExtractor extractor,
                                int trackIndex,
                                MediaFormat outputFormat,
                                MediaMuxer muxer) {
        mExtractor = extractor;
        mTrackIndex = trackIndex;
        mOutputFormat = outputFormat;
        mMuxer = muxer;
    }

    @Override
    public void setup() {
        mExtractor.selectTrack(mTrackIndex);
        mEncoder = MediaCodec.createEncoderByType(mOutputFormat.getString(MediaFormat.KEY_MIME));
        mEncoder.configure(mOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoderInputSurfaceWrapper = new InputSurface(mEncoder.createInputSurface());
        mEncoderInputSurfaceWrapper.makeCurrent();
        mEncoder.start();
        mEncoderStarted = true;
        mEncoderOutputBuffers = mEncoder.getOutputBuffers();

        MediaFormat inputFormat = mExtractor.getTrackFormat(mTrackIndex);
        mDecoderOutputSurfaceWrapper = new OutputSurface();
        mDecoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
        mDecoder.configure(inputFormat, mDecoderOutputSurfaceWrapper.getSurface(), null, 0);
        mDecoder.start();
        mDecoderStarted = true;
        mDecoderInputBuffers = mDecoder.getInputBuffers();
    }

    @Override
    public MediaFormat getDeterminedFormat() {
        return mActualOutputFormat;
    }

    @Override
    public void addTrackToMuxer() {
        if (mActualOutputFormat == null) {
            throw new IllegalStateException("Format is not determined yet.");
        }
        mMuxerTrackIndex = mMuxer.addTrack(mActualOutputFormat);
        Log.v(TAG, "Added track #" + mMuxerTrackIndex + " with " + mActualOutputFormat.getString(MediaFormat.KEY_MIME) + " to muxer");
    }

    @Override
    public void determineFormat() {
        try {
            mExtractor.selectTrack(mTrackIndex);
            while (mActualOutputFormat == null && !mIsEncoderEOS) {
                int trackIndex = mExtractor.getSampleTrackIndex();
                if (trackIndex >= 0 && trackIndex != mTrackIndex) {
                    throw new IllegalStateException("You should not select any tracks on MediaExtractor when calling determineFormat()."
                            + " Expected track " + mTrackIndex + " but received sample of track " + trackIndex + ".");
                }

                // fill pipeline
                drainExtractor(0);
                while (drainDecoder(0) == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY) ;
                while (mActualOutputFormat == null && drainEncoder(0, false) == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY) ;
            }
            if (mActualOutputFormat == null) {
                throw new IllegalStateException("Actual output format could not be determined for track: " + mTrackIndex);
            }
        } finally {
            // resetCodecs();
            // mExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC); // reset position
            // mExtractor.unselectTrack(mTrackIndex);
        }
    }

    // FIXME: busy implementation IS WRONG!
    @Override
    public boolean stepPipeline() {
        boolean busy = false;

        /*
        if (!mWritingToMuxerStarted) {
            int extractorResult;
            // NOTE: keep from crash after flushing.
            while ((extractorResult = drainExtractor(0)) == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY) { busy = true; }
            if (extractorResult != DRAIN_STATE_CONSUMED) {
                // not ready yet.
                return busy;
            }
            mWritingToMuxerStarted = true;
        }
        */

        while (drainEncoder(0, true) != DRAIN_STATE_NONE) { busy = true; }
        // NOTE: not repeating to keep from deadlock when encoder is full.
        while (drainDecoder(0) == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY) { busy = true; }
        while (drainExtractor(0) == DRAIN_STATE_CONSUMED) { busy = true; }
        return busy;
    }

    @Override
    public boolean isFinished() {
        return mIsEncoderEOS;
    }

    // TODO: CloseGuard
    @Override
    public void release() {
        if (mDecoder != null) {
            if (mDecoderStarted) mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }
        if (mEncoder != null) {
            if (mEncoderStarted) mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mDecoderOutputSurfaceWrapper != null) {
            mDecoderOutputSurfaceWrapper.release();
            mDecoderOutputSurfaceWrapper = null;
        }
        if (mEncoderInputSurfaceWrapper != null) {
            mEncoderInputSurfaceWrapper.release();
            mEncoderInputSurfaceWrapper = null;
        }
    }

    private void resetCodecs() {
        mDecoder.flush();
        mEncoder.flush();
        mIsExtractorEOS =  mIsDecoderEOS =  mIsEncoderEOS = false;
    }

    private int drainExtractor(long timeoutUs) {
        if (mIsExtractorEOS) return DRAIN_STATE_NONE;
        int trackIndex = mExtractor.getSampleTrackIndex();
        if (trackIndex >= 0 && trackIndex != mTrackIndex) {
            return DRAIN_STATE_NONE;
        }
        int result = mDecoder.dequeueInputBuffer(timeoutUs);
        if (result < 0) return DRAIN_STATE_NONE;
        if (trackIndex < 0) {
            mIsExtractorEOS = true;
            mDecoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return DRAIN_STATE_NONE;
        }
        int sampleSize = mExtractor.readSampleData(mDecoderInputBuffers[result], 0);
        boolean isKeyFrame = (mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        mDecoder.queueInputBuffer(result, 0, sampleSize, mExtractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);
        mExtractor.advance();
        return DRAIN_STATE_CONSUMED;
    }

    private int drainDecoder(long timeoutUs) {
        if (mIsDecoderEOS) return DRAIN_STATE_NONE;
        int result = mDecoder.dequeueOutputBuffer(mBufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mEncoder.signalEndOfInputStream();
            mIsDecoderEOS = true;
        }
        boolean doRender = (mBufferInfo.size > 0);
        // NOTE: doRender will block if buffer (of encoder) is full.
        // Refer: http://bigflake.com/mediacodec/CameraToMpegTest.java.txt
        mDecoder.releaseOutputBuffer(result, doRender);
        if (doRender) {
            mDecoderOutputSurfaceWrapper.awaitNewImage();
            mDecoderOutputSurfaceWrapper.drawImage();
            mEncoderInputSurfaceWrapper.setPresentationTime(mBufferInfo.presentationTimeUs * 1000);
            mEncoderInputSurfaceWrapper.swapBuffers();
        }
        return DRAIN_STATE_CONSUMED;
    }

    private int drainEncoder(long timeoutUs, boolean writeToMuxer) {
        if (mIsEncoderEOS) return DRAIN_STATE_NONE;
        int result = mEncoder.dequeueOutputBuffer(mBufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                assert mActualOutputFormat == null;
                mActualOutputFormat = mEncoder.getOutputFormat();
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                mEncoderOutputBuffers = mEncoder.getOutputBuffers();
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }
        assert mActualOutputFormat != null;

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mIsEncoderEOS = true;
            mBufferInfo.set(0, 0, 0, mBufferInfo.flags);
        }
        if (writeToMuxer) {
            mMuxer.writeSampleData(mMuxerTrackIndex, mEncoderOutputBuffers[result], mBufferInfo);
        }
        mEncoder.releaseOutputBuffer(result, false);
        return DRAIN_STATE_CONSUMED;
    }
}
