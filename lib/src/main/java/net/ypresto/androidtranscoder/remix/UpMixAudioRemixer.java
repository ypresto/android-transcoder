package net.ypresto.androidtranscoder.remix;

import java.nio.ShortBuffer;

/**
 * A {@link AudioRemixer} that upmixes mono audio to stereo.
 */
public class UpMixAudioRemixer implements AudioRemixer {

    @Override
    public void remix(final ShortBuffer inSBuff, final ShortBuffer outSBuff) {
        // Up-mix mono to stereo
        final int inRemaining = inSBuff.remaining();
        final int outSpace = outSBuff.remaining() / 2;

        final int samplesToBeProcessed = Math.min(inRemaining, outSpace);
        for (int i = 0; i < samplesToBeProcessed; ++i) {
            final short inSample = inSBuff.get();
            outSBuff.put(inSample);
            outSBuff.put(inSample);
        }
    }
}
