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

import android.media.MediaFormat;
import android.util.Log;

import net.ypresto.androidtranscoder.utils.Logger;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
* Created by yuya.tanaka on 2014/11/20.
*/
class ExportPreset960x540Strategy implements MediaFormatStrategy {
    private static final String TAG = "ExportPreset960x540Strategy";
    private static final Logger LOG = new Logger(TAG);

    @Nullable
    @Override
    public MediaFormat createVideoOutputFormat(@NonNull MediaFormat inputFormat) {
        // TODO: detect non-baseline profile and throw exception
        int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        MediaFormat outputFormat = MediaFormatPresets.getExportPreset960x540(width, height);
        int outWidth = outputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int outHeight = outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        LOG.v(String.format(Locale.getDefault(),
                "inputFormat: %dx%d => outputFormat: %dx%d", width, height, outWidth, outHeight));
        return outputFormat;
    }

    @Nullable
    @Override
    public MediaFormat createAudioOutputFormat(@NonNull MediaFormat inputFormat) {
        // TODO
        return null;
    }
}
