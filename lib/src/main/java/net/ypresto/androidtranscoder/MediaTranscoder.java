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

import net.ypresto.androidtranscoder.engine.MediaTranscoderEngine;
import net.ypresto.androidtranscoder.source.DataSource;
import net.ypresto.androidtranscoder.strategy.OutputStrategy;
import net.ypresto.androidtranscoder.utils.Logger;

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
     * Starts building transcoder options.
     * Requires a non null absolute path to the output file.
     *
     * @param outPath path to output file
     * @return an options builder
     */
    @NonNull
    public static MediaTranscoderOptions.Builder into(@NonNull String outPath) {
        return new MediaTranscoderOptions.Builder(outPath);
    }

    /**
     * Transcodes video file asynchronously.
     *
     * @param options The transcoder options.
     */
    @SuppressWarnings("WeakerAccess")
    public Future<Void> transcode(@NonNull final MediaTranscoderOptions options) {
        final Listener listenerWrapper = new ListenerWrapper(options.listenerHandler,
                options.listener, options.dataSource);
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
                            listenerWrapper.onTranscodeProgress(progress);
                        }
                    });
                    engine.setDataSource(options.dataSource);
                    engine.transcode(options.outPath, options.videoOutputStrategy, options.audioOutputStrategy);
                } catch (IOException e) {
                    LOG.w("Transcode failed: input source (" + options.dataSource.toString() + ") not found"
                            + " or could not open output file ('" + options.outPath + "') .", e);
                    caughtException = e;
                } catch (InterruptedException e) {
                    LOG.i("Cancel transcode video file.", e);
                    caughtException = e;
                } catch (RuntimeException e) {
                    LOG.e("Fatal error while transcoding, this might be invalid format or bug in engine or Android.", e);
                    caughtException = e;
                }

                final Exception exception = caughtException;
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
                if (exception != null) throw exception;
                return null;
            }
        });
        futureReference.set(createdFuture);
        return createdFuture;
    }

    /**
     * Listeners for transcoder events. All the callbacks are called on the handler
     * specified with {@link MediaTranscoderOptions.Builder#setListenerHandler(Handler)}.
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
         * @param exception Exception thrown from {@link MediaTranscoderEngine#transcode(String, OutputStrategy, OutputStrategy)}.
         *                  Note that it IS NOT {@link java.lang.Throwable}. This means {@link java.lang.Error} won't be caught.
         */
        void onTranscodeFailed(@NonNull Exception exception);
    }

    /**
     * Wraps a Listener and a DataSource object, ensuring that the source
     * is released when transcoding ends, fails or is canceled.
     *
     * It posts events on the given handler.
     */
    private static class ListenerWrapper implements Listener {

        private Handler mHandler;
        private Listener mListener;
        private DataSource mDataSource;

        private ListenerWrapper(@NonNull Handler handler, @NonNull Listener listener, @NonNull DataSource source) {
            mHandler = handler;
            mListener = listener;
            mDataSource = source;
        }

        @Override
        public void onTranscodeCanceled() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDataSource.release();
                    mListener.onTranscodeCanceled();
                }
            });
        }

        @Override
        public void onTranscodeCompleted() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDataSource.release();
                    mListener.onTranscodeCompleted();
                }
            });
        }

        @Override
        public void onTranscodeFailed(@NonNull final Exception exception) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDataSource.release();
                    mListener.onTranscodeFailed(exception);
                }
            });
        }

        @Override
        public void onTranscodeProgress(final double progress) {
            // Don't think there's a safe way to avoid this allocation?
            // Other than creating a pool of runnables.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onTranscodeProgress(progress);
                }
            });
        }
    }
}
