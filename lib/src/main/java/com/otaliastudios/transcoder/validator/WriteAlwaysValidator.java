package com.otaliastudios.transcoder.validator;

import com.otaliastudios.transcoder.engine.TrackStatus;

/**
 * A {@link Validator} that always writes to target file, no matter the track status,
 * presence of tracks and so on. The output container file might be empty or unnecessary.
 */
public class WriteAlwaysValidator implements Validator {

    @Override
    public boolean validate(TrackStatus videoStatus, TrackStatus audioStatus) {
        return true;
    }
}
