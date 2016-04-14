/*
 * Copyright (C) 2014 Yuya Tanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ypresto.androidtranscoder.format;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

public class Android720pFormatStrategy implements MediaFormatStrategy {
    private static final String TAG = "720pFormatStrategy";
    public static final int LONGER_LENGTH = 1280;
    public static final int SHORTER_LENGTH = 720;
    public static final int DEFAULT_BITRATE = 8000 * 1000; // From Nexus 4 Camera in 720p
    private final int mBitRate;
    private final int mLongerLength;
    private final int mShorterLength;
    private final String mVideoFormat = MediaFormatExtraConstants.MIMETYPE_VIDEO_AVC;

    public Android720pFormatStrategy() {
        this(DEFAULT_BITRATE);
    }

    public Android720pFormatStrategy(int bitRate) {
        this(bitRate, LONGER_LENGTH, SHORTER_LENGTH);
    }

    public Android720pFormatStrategy(int bitRate, int longerLength, int shorterLength) {
        mBitRate = bitRate;
        mLongerLength = longerLength;
        mShorterLength = shorterLength;
    }

    @Override
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        int longer, shorter, outWidth, outHeight;
        if (width >= height) {
            longer = width;
            shorter = height;
            outWidth = mLongerLength;
            outHeight = mShorterLength;
        } else {
            shorter = width;
            longer = height;
            outWidth = mShorterLength;
            outHeight = mLongerLength;
        }
        if (longer * 9 != shorter * 16) {
            throw new OutputFormatUnavailableException("This video is not 16:9, and is not able to transcode. (" + width + "x" + height + ")");
        }
        if (shorter <= mShorterLength && longer < mLongerLength) {
            Log.d(TAG, "This video is less or equal to 720p, pass-through. (" + width + "x" + height + ")");
            return null;
        }
        MediaFormat format = MediaFormat.createVideoFormat(mVideoFormat, outWidth, outHeight);
        // From Nexus 4 Camera in 720p
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        return format;
    }

    @Override
    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        return null;
    }
}
