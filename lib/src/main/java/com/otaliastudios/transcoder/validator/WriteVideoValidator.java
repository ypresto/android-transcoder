package com.otaliastudios.transcoder.validator;

import com.otaliastudios.transcoder.engine.TrackStatus;

/**
 * A {@link Validator} that gives priority to the video track.
 * Transcoding will not happen if the video track does not need it, even if the
 * audio track might need it.
 */
public class WriteVideoValidator implements Validator {

    @Override
    public boolean validate(TrackStatus videoStatus, TrackStatus audioStatus) {
        switch (videoStatus) {
            case ABSENT: return false;
            case REMOVING: return true;
            case COMPRESSING: return true;
            case PASS_THROUGH: return false;
        }
        return true;
    }
}
