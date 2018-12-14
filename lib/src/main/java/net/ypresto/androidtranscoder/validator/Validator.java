package net.ypresto.androidtranscoder.validator;

/**
 * A validator determines if the transcoding process should proceed or not,
 * after the {@link net.ypresto.androidtranscoder.strategy.OutputStrategy} have
 * provided the output format.
 */
public interface Validator {

    /**
     * Return true if the transcoding should proceed, false otherwise.
     *
     * @param videoStatus the status of the video track
     * @param audioStatus the status of the audio track
     * @return true to proceed
     */
    boolean validate(TrackStatus videoStatus, TrackStatus audioStatus);

    public enum TrackStatus {
        ABSENT, REMOVING, PASS_THROUGH, COMPRESSING
    }
}
