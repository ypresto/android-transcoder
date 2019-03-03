package com.otaliastudios.transcoder.source;

import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;

import java.io.IOException;

/**
 * Represents the source of input data.
 */
public interface DataSource {

    void apply(MediaExtractor extractor) throws IOException;

    void apply(MediaMetadataRetriever retriever);

    void release();
}
