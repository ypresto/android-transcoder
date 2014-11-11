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
package net.ypresto.androidtranscoder;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import net.ypresto.androidtranscoder.engine.MediaTranscoderEngine;
import net.ypresto.androidtranscoder.format.MediaFormatPresets;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MediaTranscoder {
    private static final String TAG = "MediaTranscoder";
    private static final int MAXIMUM_THREAD = 1; // TODO
    private static volatile MediaTranscoder sMediaTranscoder;
    private ThreadPoolExecutor mExecutor;

    private MediaTranscoder() {
        mExecutor = new ThreadPoolExecutor(
                0, MAXIMUM_THREAD, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "MediaTranscoder-Worker");
                    }
                });
    }

    public static MediaTranscoder getInstance() {
        if (sMediaTranscoder == null) {
            synchronized (MediaTranscoder.class) {
                if (sMediaTranscoder == null) {
                    sMediaTranscoder = new MediaTranscoder();
                }
            }
        }
        return sMediaTranscoder;
    }

    /**
     * Transcodes video file asynchronously.
     *
     * @param inFileDescriptor FileDescriptor for input.
     * @param outPath          File path for output.
     * @param listener         Listener instance for callback.
     */
    public void transcodeVideo(final FileDescriptor inFileDescriptor, final String outPath, final Listener listener) {
        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        final Handler handler = new Handler(looper);
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Exception caughtException = null;
                try {
                    MediaTranscoderEngine engine = new MediaTranscoderEngine();
                    engine.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                        @Override
                        public void onProgress(final double progress) {
                            handler.post(new Runnable() { // TODO: reuse instance
                                @Override
                                public void run() {
                                    listener.onTranscodeProgress(progress);
                                }
                            });
                        }
                    });
                    engine.setDataSource(inFileDescriptor);
                    engine.transcodeVideo(outPath, MediaFormatPresets.getExportPreset960x540());
                } catch (IOException e) {
                    Log.w(TAG, "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found"
                            + " or could not open output file ('" + outPath + "') .", e);
                    caughtException = e;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Fatal error while transcoding, this might be invalid output format or bug in engine or Android.", e);
                    caughtException = e;
                }

                final Exception exception = caughtException;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            listener.onTranscodeCompleted();
                        } else {
                            listener.onTranscodeFailed(exception);
                        }
                    }
                });
            }
        });
    }

    public interface Listener {
        /**
         * Called to notify progress.
         *
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        void onTranscodeProgress(double progress);

        /**
         * Called when transcode completed.
         */
        void onTranscodeCompleted();

        /**
         * Called when transcode failed.
         *
         * @param exception Exception caused failure. Note that it IS NOT {@link java.lang.Throwable}.
         *                  This means {@link java.lang.Error} won't be caught.
         */
        void onTranscodeFailed(Exception exception);
    }
}
