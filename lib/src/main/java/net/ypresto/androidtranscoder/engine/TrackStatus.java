package net.ypresto.androidtranscoder.engine;

public enum TrackStatus {
    ABSENT, REMOVING, PASS_THROUGH, COMPRESSING;

    /**
     * This is used to understand whether we should select this track
     * in MediaExtractor, and add this track to MediaMuxer.
     * Basically if it should be read and written or not
     * (no point in just reading without writing).
     */
    public boolean isTranscoding() {
        switch (this) {
            case PASS_THROUGH: return true;
            case COMPRESSING: return true;
            case REMOVING: return false;
            case ABSENT: return false;
        }
        throw new RuntimeException("Unexpected track status: " + this);
    }
}
