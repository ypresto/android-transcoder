package net.ypresto.androidtranscoder.strategy;

/**
 * A {@link DefaultVideoStrategy} that uses 360x480 (3:4).
 */
public class Default360pVideoStrategy extends DefaultVideoStrategy {

    // Bitrate: https://developer.android.com/guide/topics/media/media-formats
    public Default360pVideoStrategy() {
        super(360, 480, 500L * 1000);
    }
}
