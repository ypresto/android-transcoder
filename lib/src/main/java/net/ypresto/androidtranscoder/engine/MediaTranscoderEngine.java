package net.ypresto.androidtranscoder.engine;

import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;

import net.ypresto.androidtranscoder.utils.MediaExtractorUtils;

import java.io.FileDescriptor;
import java.io.IOException;

// TODO: treat encrypted data
public class MediaTranscoderEngine {
    private static final MediaFormat OUTPUT_VIDEO_FORMAT;
    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 100;

    static {
        // Refer: https://gist.github.com/wobbals/3990442
        // Refer: https://developer.apple.com/library/ios/documentation/networkinginternet/conceptual/streamingmediaguide/UsingHTTPLiveStreaming/UsingHTTPLiveStreaming.html#//apple_ref/doc/uid/TP40008332-CH102-SW8
        // Refer: (ANDROID ROOT)/media/libstagefright/ACodec.cpp
        /*
        OUTPUT_VIDEO_FORMAT = MediaFormat.createVideoFormat("video/avc", 640, 480); // TODO
        OUTPUT_VIDEO_FORMAT.setInteger(MediaFormat.KEY_BIT_RATE, 5375 * 1000); // TODO
        OUTPUT_VIDEO_FORMAT.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        OUTPUT_VIDEO_FORMAT.setFloat(MediaFormat.KEY_FRAME_RATE, 29.97f); // NTSC, recommended by apple
        OUTPUT_VIDEO_FORMAT.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); // FIXME
        */
        OUTPUT_VIDEO_FORMAT = MediaFormat.createVideoFormat("video/avc", 320, 240); // TODO
        OUTPUT_VIDEO_FORMAT.setInteger(MediaFormat.KEY_BIT_RATE, 2000000); // TODO
        OUTPUT_VIDEO_FORMAT.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        OUTPUT_VIDEO_FORMAT.setInteger(MediaFormat.KEY_FRAME_RATE, 15); // NTSC, recommended by apple
        OUTPUT_VIDEO_FORMAT.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10); // FIXME
    }

    private FileDescriptor mInputFileDescriptor;
    private TrackTranscoder mVideoTrackTranscoder;
    private TrackTranscoder mAudioTrackTranscoder;
    private MediaExtractor mExtractor;
    private MediaExtractor mExtractor2;
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
    public void transcode(String outputPath) throws IOException {
        if (outputPath == null) {
            throw new NullPointerException("Output path cannot be null.");
        }
        if (mInputFileDescriptor == null) {
            throw new IllegalStateException("Data source is not set.");
        }
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mInputFileDescriptor);
            mExtractor2 = new MediaExtractor();
            mExtractor2.setDataSource(mInputFileDescriptor);
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            setupTrackTranscoders();
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
            if (mExtractor2 != null) {
                mExtractor2.release();
                mExtractor2 = null;
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

    private void setupTrackTranscoders() {
        MediaExtractorUtils.TrackResult trackResult = MediaExtractorUtils.getFirstVideoAndAudioTrack(mExtractor);
        mVideoTrackTranscoder = new VideoTrackTranscoder(mExtractor, trackResult.mVideoTrackIndex, OUTPUT_VIDEO_FORMAT, mMuxer);
        mVideoTrackTranscoder.setup();
        mAudioTrackTranscoder = new PassThroughTrackTranscoder(mExtractor2, trackResult.mAudioTrackIndex, mMuxer);
        mAudioTrackTranscoder.setup();
        mVideoTrackTranscoder.determineFormat();
        mAudioTrackTranscoder.determineFormat();
        mVideoTrackTranscoder.addTrackToMuxer();
        mAudioTrackTranscoder.addTrackToMuxer();
        mExtractor.selectTrack(trackResult.mVideoTrackIndex);
        mExtractor2.selectTrack(trackResult.mAudioTrackIndex);
    }

    private void runPipelines() {
        while (!(mVideoTrackTranscoder.isFinished() && mAudioTrackTranscoder.isFinished())) {
            boolean stepped = mVideoTrackTranscoder.stepPipeline();
            stepped |= mAudioTrackTranscoder.stepPipeline();
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
                } catch (InterruptedException e) {
                    // nothing to do
                }
            }
        }
    }
}
