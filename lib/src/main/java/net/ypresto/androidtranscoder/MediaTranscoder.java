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

import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import net.ypresto.androidtranscoder.engine.MediaTranscoderEngine;
import net.ypresto.androidtranscoder.engine.MediaTrimTime;
import net.ypresto.androidtranscoder.format.MediaFormatPresets;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
     * Audio track will be kept unchanged.
     *
     * @param inFileDescriptor FileDescriptor for input.
     * @param outPath          File path for output.
     * @param listener         Listener instance for callback.
     * @deprecated Use {@link #transcodeVideo(FileDescriptor, String, MediaFormatStrategy, MediaTranscoder.Listener)} which accepts output video format.
     */
    @Deprecated
    public Future<Void> transcodeVideo(final FileDescriptor inFileDescriptor, final String outPath, final Listener listener) {
        return transcodeVideo(inFileDescriptor, outPath, new MediaFormatStrategy() {
            @Override
            public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
                return MediaFormatPresets.getExportPreset960x540();
            }

            @Override
            public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
                return null;
            }
        }, null, listener);
    }

    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     *
     * @param inPath            File path for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param listener          Listener instance for callback.
     * @throws IOException if input file could not be read.
     */
    public Future<Void> transcodeVideo(final String inPath, final String outPath, final MediaFormatStrategy outFormatStrategy, final Listener listener) throws IOException {
        FileInputStream fileInputStream = null;
        FileDescriptor inFileDescriptor;
        try {
            fileInputStream = new FileInputStream(inPath);
            inFileDescriptor = fileInputStream.getFD();
        } catch (IOException e) {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException eClose) {
                    Log.e(TAG, "Can't close input stream: ", eClose);
                }
            }
            throw e;
        }
        final FileInputStream finalFileInputStream = fileInputStream;
        return transcodeVideo(inFileDescriptor, outPath, outFormatStrategy, null, new Listener() {
            @Override
            public void onTranscodeProgress(double progress) {
                listener.onTranscodeProgress(progress);
            }

            @Override
            public void onTranscodeCompleted() {
                listener.onTranscodeCompleted();
                closeStream();
            }

            @Override
            public void onTranscodeCanceled() {
                listener.onTranscodeCanceled();
                closeStream();
            }

            @Override
            public void onTranscodeFailed(Exception exception) {
                listener.onTranscodeFailed(exception);
                closeStream();
            }

            private void closeStream() {
                try {
                    finalFileInputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Can't close input stream: ", e);
                }
            }
        });
    }

    /**
     * Transcodes video file asynchronously.
     * Audio track will be kept unchanged.
     *
     * @param inFileDescriptor  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param mediaTrimTime Media trim time.
     * @param listener          Listener instance for callback.
     */
    public Future<Void> transcodeVideo(final FileDescriptor inFileDescriptor, final String outPath, final MediaFormatStrategy outFormatStrategy, final MediaTrimTime mediaTrimTime, final Listener listener) {
        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        final Handler handler = new Handler(looper);
        final AtomicReference<Future<Void>> futureReference = new AtomicReference<>();
        final Future<Void> createdFuture = mExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
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
                    engine.transcodeVideo(outPath, outFormatStrategy, mediaTrimTime);
                } catch (IOException e) {
                    Log.w(TAG, "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found"
                            + " or could not open output file ('" + outPath + "') .", e);
                    caughtException = e;
                } catch (InterruptedException e) {
                    Log.i(TAG, "Cancel transcode video file.", e);
                    caughtException = e;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
                    caughtException = e;
                }

                final Exception exception = caughtException;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            listener.onTranscodeCompleted();
                        } else {
                            Future<Void> future = futureReference.get();
                            if (future != null && future.isCancelled()) {
                                listener.onTranscodeCanceled();
                            } else {
                                listener.onTranscodeFailed(exception);
                            }
                        }
                    }
                });

                if (exception != null) throw exception;
                return null;
            }
        });
        futureReference.set(createdFuture);
        return createdFuture;
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
         * Called when transcode canceled.
         */
        void onTranscodeCanceled();

        /**
         * Called when transcode failed.
         *
         * @param exception Exception thrown from {@link MediaTranscoderEngine#transcodeVideo(String, MediaFormatStrategy)}.
         *                  Note that it IS NOT {@link java.lang.Throwable}. This means {@link java.lang.Error} won't be caught.
         */
        void onTranscodeFailed(Exception exception);
    }
}
