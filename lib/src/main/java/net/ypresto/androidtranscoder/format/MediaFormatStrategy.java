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

public interface MediaFormatStrategy {

    /**
     * Returns preferred video format for encoding.
     *
     * @param inputFormat MediaFormat from MediaExtractor, contains csd-0/csd-1.
     * @return null for passthrough.
     * @throws OutputFormatUnavailableException if input could not be transcoded because of restrictions.
     */
    MediaFormat createVideoOutputFormat(MediaFormat inputFormat);

    /**
     * Caution: this method should return null currently.
     *
     * @return null for passthrough.
     * @throws OutputFormatUnavailableException if input could not be transcoded because of restrictions.
     */
    MediaFormat createAudioOutputFormat(MediaFormat inputFormat);

}
