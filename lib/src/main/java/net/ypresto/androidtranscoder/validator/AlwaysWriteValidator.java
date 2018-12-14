package net.ypresto.androidtranscoder.validator;

/**
 * A {@link Validator} that always writes to target file, no matter the track status,
 * presence of tracks and so on. The output container file might be empty or unnecessary.
 */
public class AlwaysWriteValidator implements Validator {

    @Override
    public boolean validate(TrackStatus videoStatus, TrackStatus audioStatus) {
        return true;
    }
}
