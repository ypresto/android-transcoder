package net.ypresto.androidtranscoder.engine;

/**
 * Created by Taishan Lin on 1/28/16.
 */
public class MediaTrimTime {
  public static final long DO_NOT_TRIM_TIME = -1;
  public final long startTimeInUs;
  public final long endTimeInUs;

  public MediaTrimTime() {
    this.startTimeInUs = DO_NOT_TRIM_TIME;
    this.endTimeInUs = DO_NOT_TRIM_TIME;
  }

  public MediaTrimTime(long startTimeInUs, long endTimeInUs) {
    this.startTimeInUs = startTimeInUs;
    this.endTimeInUs = endTimeInUs;
    if (endTimeInUs != DO_NOT_TRIM_TIME && startTimeInUs >= endTimeInUs) {
      throw new IllegalArgumentException("Start time is larger than or equal to end time!");
    }
  }
}
