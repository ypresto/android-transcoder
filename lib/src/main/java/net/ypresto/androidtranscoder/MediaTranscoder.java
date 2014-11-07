package net.ypresto.androidtranscoder;

import android.os.Handler;
import android.util.Log;

import net.ypresto.androidtranscoder.engine.MediaTranscoderEngine;
import net.ypresto.androidtranscoder.format.MediaFormatPresets;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MediaTranscoder {
    private static final String TAG = "MediaTranscoder";
    private static volatile MediaTranscoder sMediaTranscoder;
    private ExecutorService mExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "MediaTranscoder-Worker");
        }
    }); // TODO

    private MediaTranscoder() {
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
        final Handler handler = new Handler();
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
