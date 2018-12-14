package net.ypresto.androidtranscoder;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import net.ypresto.androidtranscoder.format.MediaFormatStrategy;
import net.ypresto.androidtranscoder.source.DataSource;
import net.ypresto.androidtranscoder.source.FileDescriptorDataSource;
import net.ypresto.androidtranscoder.source.FilePathDataSource;
import net.ypresto.androidtranscoder.source.UriDataSource;

import java.io.FileDescriptor;
import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Collects transcoding options consumed by {@link MediaTranscoder}.
 */
public class MediaTranscoderOptions {

    private MediaTranscoderOptions() {}

    String outPath;
    DataSource dataSource;
    MediaFormatStrategy formatStrategy;
    MediaTranscoder.Listener listener;
    Handler listenerHandler;

    public static class Builder {
        private String outPath;
        private DataSource dataSource;
        private MediaFormatStrategy formatStrategy;
        private MediaTranscoder.Listener listener;
        private Handler listenerHandler;

        Builder(@NonNull String outPath) {
            this.outPath = outPath;
        }

        public Builder setDataSource(@NonNull DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder setDataSource(@NonNull FileDescriptor fileDescriptor) {
            this.dataSource = new FileDescriptorDataSource(fileDescriptor);
            return this;
        }

        public Builder setDataSource(@NonNull String inPath) {
            this.dataSource = new FilePathDataSource(inPath);
            return this;
        }

        public Builder setDataSource(@NonNull Context context, @NonNull Uri uri) {
            this.dataSource = new UriDataSource(context, uri);
            return this;
        }

        public Builder setMediaFormatStrategy(@NonNull MediaFormatStrategy formatStrategy) {
            this.formatStrategy = formatStrategy;
            return this;
        }

        public Builder setListener(@NonNull MediaTranscoder.Listener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Sets an handler for {@link MediaTranscoder.Listener} callbacks.
         * If null, this will default to the thread that starts the transcoding, if it
         * has a looper, or the UI thread otherwise.
         *
         * @param listenerHandler the thread to receive callbacks
         * @return this for chaining
         */
        public Builder setListenerHandler(@Nullable Handler listenerHandler) {
            this.listenerHandler = listenerHandler;
            return this;
        }

        @SuppressWarnings("WeakerAccess")
        public MediaTranscoderOptions build() {
            if (listener == null) throw new IllegalStateException("listener can't be null");
            if (dataSource == null) throw new IllegalStateException("data source can't be null");
            if (outPath == null) throw new IllegalStateException("out path can't be null");
            if (formatStrategy == null) throw new IllegalStateException("format strategy can't be null");
            if (listenerHandler == null) {
                Looper looper = Looper.myLooper();
                if (looper == null) looper = Looper.getMainLooper();
                listenerHandler = new Handler(looper);
            }
            MediaTranscoderOptions options = new MediaTranscoderOptions();
            options.listener = listener;
            options.dataSource = dataSource;
            options.outPath = outPath;
            options.formatStrategy = formatStrategy;
            return options;
        }

        public Future<Void> transcode() {
            return MediaTranscoder.getInstance().transcode(build());
        }
    }
}
