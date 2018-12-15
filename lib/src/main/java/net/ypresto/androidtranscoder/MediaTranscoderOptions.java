package net.ypresto.androidtranscoder;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import net.ypresto.androidtranscoder.source.DataSource;
import net.ypresto.androidtranscoder.source.FileDescriptorDataSource;
import net.ypresto.androidtranscoder.source.FilePathDataSource;
import net.ypresto.androidtranscoder.source.UriDataSource;
import net.ypresto.androidtranscoder.strategy.DefaultAudioStrategy;
import net.ypresto.androidtranscoder.strategy.DefaultVideoStrategies;
import net.ypresto.androidtranscoder.strategy.OutputStrategy;
import net.ypresto.androidtranscoder.validator.DefaultValidator;
import net.ypresto.androidtranscoder.validator.Validator;

import java.io.FileDescriptor;
import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Collects transcoding options consumed by {@link MediaTranscoder}.
 */
@SuppressWarnings("WeakerAccess")
public class MediaTranscoderOptions {

    private MediaTranscoderOptions() {}

    public String outPath;
    public DataSource dataSource;
    public OutputStrategy audioOutputStrategy;
    public OutputStrategy videoOutputStrategy;
    public MediaTranscoder.Listener listener;
    public Handler listenerHandler;
    public Validator validator;

    public static class Builder {
        private String outPath;
        private DataSource dataSource;
        private MediaTranscoder.Listener listener;
        private Handler listenerHandler;
        private OutputStrategy audioOutputStrategy;
        private OutputStrategy videoOutputStrategy;
        private Validator validator;

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

        /**
         * Sets the audio output strategy. If absent, this defaults to
         * {@link net.ypresto.androidtranscoder.strategy.DefaultAudioStrategy}.
         *
         * @param outputStrategy the desired strategy
         * @return this for chaining
         */
        public Builder setAudioOutputStrategy(@Nullable OutputStrategy outputStrategy) {
            this.audioOutputStrategy = outputStrategy;
            return this;
        }

        /**
         * Sets the video output strategy. If absent, this defaults to the 16:9
         * strategy returned by {@link DefaultVideoStrategies#for720x1280()}.
         *
         * @param outputStrategy the desired strategy
         * @return this for chaining
         */
        public Builder setVideoOutputStrategy(@Nullable OutputStrategy outputStrategy) {
            this.videoOutputStrategy = outputStrategy;
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

        /**
         * Sets a validator to understand whether the transcoding process should
         * stop before being started, based on the tracks status. Will default to
         * {@link net.ypresto.androidtranscoder.validator.DefaultValidator}.
         *
         * @param validator the validator
         * @return this for chaining
         */
        public Builder setValidator(@Nullable Validator validator) {
            this.validator = validator;
            return this;
        }

        @SuppressWarnings("WeakerAccess")
        public MediaTranscoderOptions build() {
            if (listener == null) throw new IllegalStateException("listener can't be null");
            if (dataSource == null) throw new IllegalStateException("data source can't be null");
            if (outPath == null) throw new IllegalStateException("out path can't be null");
            if (listenerHandler == null) {
                Looper looper = Looper.myLooper();
                if (looper == null) looper = Looper.getMainLooper();
                listenerHandler = new Handler(looper);
            }
            if (audioOutputStrategy == null) audioOutputStrategy = new DefaultAudioStrategy(DefaultAudioStrategy.AUDIO_CHANNELS_AS_IS);
            if (videoOutputStrategy == null) videoOutputStrategy = DefaultVideoStrategies.for720x1280();
            if (validator == null) validator = new DefaultValidator();
            MediaTranscoderOptions options = new MediaTranscoderOptions();
            options.listener = listener;
            options.dataSource = dataSource;
            options.outPath = outPath;
            options.listenerHandler = listenerHandler;
            options.audioOutputStrategy = audioOutputStrategy;
            options.videoOutputStrategy = videoOutputStrategy;
            options.validator = validator;
            return options;
        }

        public Future<Void> transcode() {
            return MediaTranscoder.getInstance().transcode(build());
        }
    }
}
