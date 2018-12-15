package net.ypresto.androidtranscoder.remix;

import java.nio.ShortBuffer;

/**
 * Remixes audio data. See {@link DownMixAudioRemixer},
 * {@link UpMixAudioRemixer} or {@link PassThroughAudioRemixer}
 * for concrete implementations.
 */
public interface AudioRemixer {

    void remix(final ShortBuffer inSBuff, final ShortBuffer outSBuff);

    AudioRemixer DOWNMIX = new DownMixAudioRemixer();

    AudioRemixer UPMIX = new UpMixAudioRemixer();

    AudioRemixer PASSTHROUGH = new PassThroughAudioRemixer();
}
