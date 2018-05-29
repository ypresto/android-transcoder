/*
 * Copyright (C) 2015 Yuya Tanaka
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
package net.ypresto.androidtranscoder.engine;

import android.media.MediaFormat;

import net.ypresto.androidtranscoder.format.MediaFormatExtraConstants;
import net.ypresto.androidtranscoder.utils.AvcCsdUtils;
import net.ypresto.androidtranscoder.utils.AvcSpsUtils;

import java.nio.ByteBuffer;

class MediaFormatValidator {
    // Refer: http://en.wikipedia.org/wiki/H.264/MPEG-4_AVC#Profiles
    private static final byte PROFILE_IDC_BASELINE = 66;

    public static void validateVideoOutputFormat(MediaFormat format) {
        String mime = format.getString(MediaFormat.KEY_MIME);
        // Refer: http://developer.android.com/guide/appendix/media-formats.html#core
        // Refer: http://en.wikipedia.org/wiki/MPEG-4_Part_14#Data_streams
        if (!MediaFormatExtraConstants.MIMETYPE_VIDEO_AVC.equals(mime)) {
            throw new InvalidOutputFormatException("Video codecs other than AVC is not supported, actual mime type: " + mime);
        }
        ByteBuffer spsBuffer = AvcCsdUtils.getSpsBuffer(format);
        byte profileIdc = AvcSpsUtils.getProfileIdc(spsBuffer);
        if (profileIdc != PROFILE_IDC_BASELINE) {
            throw new InvalidOutputFormatException("Non-baseline AVC video profile is not supported by Android OS, actual profile_idc: " + profileIdc);
        }
    }

    public static void validateAudioOutputFormat(MediaFormat format) {
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (!MediaFormatExtraConstants.MIMETYPE_AUDIO_AAC.equals(mime) && !MediaFormatExtraConstants.MIMETYPE_AUDIO_AMR.equals(mime)) {
            throw new InvalidOutputFormatException("Audio codecs other than AAC/AMR is not supported, actual mime type: " + mime);
        }
    }
}
