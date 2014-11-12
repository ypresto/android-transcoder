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

import junit.framework.TestCase;

public class MediaFormatPresetsTest extends TestCase {

    public void testGetExportPreset960x540() throws Exception {
        assertNull(MediaFormatPresets.getExportPreset960x540(960, 540));
        assertNull(MediaFormatPresets.getExportPreset960x540(540, 960));
        assertNull(MediaFormatPresets.getExportPreset960x540(480, 320));
        assertWidthAndHeightEquals(MediaFormatPresets.getExportPreset960x540(1024, 768), 960, 720);
        assertWidthAndHeightEquals(MediaFormatPresets.getExportPreset960x540(768, 1024), 720, 960);
        assertWidthAndHeightEquals(MediaFormatPresets.getExportPreset960x540(768, 1024), 720, 960);
        assertWidthAndHeightEquals(MediaFormatPresets.getExportPreset960x540(1920, 1080), 960, 540);
        assertWidthAndHeightEquals(MediaFormatPresets.getExportPreset960x540(1080, 1920), 540, 960);
        assertWidthAndHeightEquals(MediaFormatPresets.getExportPreset960x540(1280, 720), 960, 540);
        assertWidthAndHeightEquals(MediaFormatPresets.getExportPreset960x540(720, 1280), 540, 960);
        try {
            MediaFormatPresets.getExportPreset960x540(1024, 769);
            fail("should throw if indivisible");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getStackTrace()[0].getClassName().equals(MediaFormatPresets.class.getName()));
        }
    }

    private void assertWidthAndHeightEquals(MediaFormat format, int width, int height) throws Exception {
        assertEquals(width, format.getInteger(MediaFormat.KEY_WIDTH));
        assertEquals(height, format.getInteger(MediaFormat.KEY_HEIGHT));
    }
}
