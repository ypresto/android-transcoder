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
package net.ypresto.androidtranscoder.utils;

import android.media.MediaFormat;

import net.ypresto.androidtranscoder.format.MediaFormatExtraConstants;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class AvcCsdUtils {
    // Refer: https://android.googlesource.com/platform/frameworks/av/+/lollipop-release/media/libstagefright/MediaCodec.cpp#2198
    private static final byte[] AVC_CSD_PREFIX = {0x00, 0x00, 0x00, 0x01};
    // Refer: http://www.cardinalpeak.com/blog/the-h-264-sequence-parameter-set/
    private static final byte AVC_SPS_NAL = 103; // 0<<7 + 3<<5 + 7<<0

    public static ByteBuffer getSpsBuffer(MediaFormat format) {
        ByteBuffer prefixedSpsBuffer = format.getByteBuffer(MediaFormatExtraConstants.KEY_AVC_SPS).asReadOnlyBuffer();
        byte[] csdPrefix = new byte[4];
        prefixedSpsBuffer.get(csdPrefix);
        if (!Arrays.equals(csdPrefix, AVC_CSD_PREFIX)) {
            throw new IllegalStateException("Wrong csd-0 prefix.");
        }
        if (prefixedSpsBuffer.get() != AVC_SPS_NAL) {
            throw new IllegalStateException("Got non SPS NAL data.");
        }
        return prefixedSpsBuffer.slice();
    }

    private AvcCsdUtils() {
        throw new RuntimeException();
    }
}
