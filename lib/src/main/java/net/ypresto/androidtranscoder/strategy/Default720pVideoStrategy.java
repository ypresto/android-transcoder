package net.ypresto.androidtranscoder.strategy;

/**
 * A {@link DefaultVideoStrategy} that uses 720x1280.
 * This preset is ensured to work on any Android &gt;=4.3 devices by Android CTS,
 * assuming that the codec is available.
 */
public class Default720pVideoStrategy extends DefaultVideoStrategy {

    // Bitrate: https://developer.android.com/guide/topics/media/media-formats
    public Default720pVideoStrategy() {
        super(720, 1280, 2L * 1000 * 1000);
    }
}
