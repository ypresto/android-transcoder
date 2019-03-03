package com.otaliastudios.transcoder.remix;

import java.nio.ShortBuffer;

/**
 * The simplest {@link AudioRemixer} that does nothing.
 */
public class PassThroughAudioRemixer implements AudioRemixer {

    @Override
    public void remix(final ShortBuffer inSBuff, final ShortBuffer outSBuff) {
        // Passthrough
        outSBuff.put(inSBuff);
    }
}
