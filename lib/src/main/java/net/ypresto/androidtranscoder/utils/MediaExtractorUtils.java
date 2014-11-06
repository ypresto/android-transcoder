package net.ypresto.androidtranscoder.utils;

import android.media.MediaExtractor;
import android.media.MediaFormat;

public class MediaExtractorUtils {

    private MediaExtractorUtils() {
    }

    public static class TrackResult {

        private TrackResult() {
        }

        public int mVideoTrackIndex;
        public String mVideoTrackMime;
        public MediaFormat mVideoTrackFormat;
        public int mAudioTrackIndex;
        public String mAudioTrackMime;
        public MediaFormat mAudioTrackFormat;
    }

    public static TrackResult getFirstVideoAndAudioTrack(MediaExtractor extractor) {
        TrackResult trackResult = new TrackResult();
        trackResult.mVideoTrackIndex = -1;
        trackResult.mAudioTrackIndex = -1;
        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (trackResult.mVideoTrackIndex < 0 && mime.startsWith("video/")) {
                trackResult.mVideoTrackIndex = i;
                trackResult.mVideoTrackMime = mime;
                trackResult.mVideoTrackFormat = format;
            } else if (trackResult.mAudioTrackIndex < 0 && mime.startsWith("audio/")) {
                trackResult.mAudioTrackIndex = i;
                trackResult.mAudioTrackMime = mime;
                trackResult.mAudioTrackFormat = format;
            }
            if (trackResult.mVideoTrackIndex >= 0 && trackResult.mAudioTrackIndex >= 0) break;
        }
        if (trackResult.mVideoTrackIndex < 0 || trackResult.mAudioTrackIndex < 0) {
            throw new IllegalArgumentException("extractor does not contain video and/or audio tracks.");
        }
        return trackResult;
    }
}
