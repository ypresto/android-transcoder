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
import net.ypresto.androidtranscoder.format.MediaFormatPresets;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;
import net.ypresto.androidtranscoder.source.DataSource;
import net.ypresto.androidtranscoder.source.FileDescriptorDataSource;
import net.ypresto.androidtranscoder.source.FilePathDataSource;
import net.ypresto.androidtranscoder.utils.Logger;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;

public class MediaTranscoder {
    private static final String TAG = "MediaTranscoder";
    private static final Logger LOG = new Logger(TAG);

    private static volatile MediaTranscoder sMediaTranscoder;

    private class Factory implements ThreadFactory {
        private AtomicInteger count = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            return new Thread(runnable, TAG + " Thread #" + count.getAndIncrement());
        }
    }

    private ThreadPoolExecutor mExecutor;

    private MediaTranscoder() {
        // This executor will execute at most 'pool' tasks concurrently,
        // then queue all the others. CPU + 1 is used by AsyncTask.
        int pool = Runtime.getRuntime().availableProcessors() + 1;
        mExecutor = new ThreadPoolExecutor(pool, pool,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new Factory());
    }

    @NonNull
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
     * @param inPath            File path for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param listener          Listener instance for callback.
     */
    @SuppressWarnings("WeakerAccess")
    public Future<Void> transcodeVideo(@NonNull final String inPath,
                                       @NonNull final String outPath,
                                       @NonNull final MediaFormatStrategy outFormatStrategy,
                                       @NonNull final Listener listener) {
        return transcodeVideo(new FilePathDataSource(inPath), outPath, outFormatStrategy, listener);
    }

    /**
     * Transcodes video file asynchronously.
     *
     * @param inFileDescriptor  FileDescriptor for input.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param listener          Listener instance for callback.
     */
    public Future<Void> transcodeVideo(@NonNull final FileDescriptor inFileDescriptor,
                                       @NonNull final String outPath,
                                       @NonNull final MediaFormatStrategy outFormatStrategy,
                                       @NonNull final Listener listener) {
        return transcodeVideo(new FileDescriptorDataSource(inFileDescriptor),
                outPath, outFormatStrategy, listener);
    }

    /**
     * Transcodes video file asynchronously.
     *
     * @param dataSource        The input data source.
     * @param outPath           File path for output.
     * @param outFormatStrategy Strategy for output video format.
     * @param listener          Listener instance for callback.
     */
    public Future<Void> transcodeVideo(@NonNull final DataSource dataSource,
                                       @NonNull final String outPath,
                                       @NonNull final MediaFormatStrategy outFormatStrategy,
                                       @NonNull Listener listener) {
        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        final Listener listenerWrapper = new ListenerWrapper(listener, dataSource);
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
                                    listenerWrapper.onTranscodeProgress(progress);
                                }
                            });
                        }
                    });
                    engine.setDataSource(dataSource);
                    engine.transcodeVideo(outPath, outFormatStrategy);
                } catch (IOException e) {
                    LOG.w("Transcode failed: input source (" + dataSource.toString() + ") not found"
                            + " or could not open output file ('" + outPath + "') .", e);
                    caughtException = e;
                } catch (InterruptedException e) {
                    LOG.i("Cancel transcode video file.", e);
                    caughtException = e;
                } catch (RuntimeException e) {
                    LOG.e("Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
                    caughtException = e;
                }

                final Exception exception = caughtException;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (exception == null) {
                            listenerWrapper.onTranscodeCompleted();
                        } else {
                            Future<Void> future = futureReference.get();
                            if (future != null && future.isCancelled()) {
                                listenerWrapper.onTranscodeCanceled();
                            } else {
                                listenerWrapper.onTranscodeFailed(exception);
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

    /**
     * Listeners for transcoder events. All the callbacks are called on the thread
     * that invoked {@link #transcodeVideo(String, String, MediaFormatStrategy, Listener)}
     * if it has a looper, otherwise on the UI thread.
     */
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
        void onTranscodeFailed(@NonNull Exception exception);
    }

    /**
     * Wraps a Listener and a DataSource object, ensuring that the source
     * is released when transcoding ends, fails or is canceled.
     */
    private class ListenerWrapper implements Listener {

        private Listener mListener;
        private DataSource mDataSource;

        private ListenerWrapper(@NonNull Listener listener, @NonNull DataSource source) {
            mListener = listener;
            mDataSource = source;
        }

        @Override
        public void onTranscodeCanceled() {
            mDataSource.release();
            mListener.onTranscodeCanceled();
        }

        @Override
        public void onTranscodeCompleted() {
            mDataSource.release();
            mListener.onTranscodeCompleted();
        }

        @Override
        public void onTranscodeFailed(@NonNull Exception exception) {
            mDataSource.release();
            mListener.onTranscodeFailed(exception);
        }

        @Override
        public void onTranscodeProgress(double progress) {
            mListener.onTranscodeProgress(progress);
        }
    }
}
