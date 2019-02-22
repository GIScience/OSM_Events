package org.heigit.bigspatialdata.eventfinder;

import java.util.HashMap;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class MappingEvent extends MappingMonth {

  private final float change;

  private final int deltakontrib;

  private final OSHDBTimestamp timestap;

  MappingEvent(OSHDBTimestamp key, MappingMonth value, int i, float f) {
    super(value.get_contributions(), value.getUser_counts(), value.get_type_counts());
    this.timestap = key;
    this.deltakontrib = i;
    this.change = f;
  }

  public float getChange() {
    return change;
  }

  public int getDeltakontrib() {
    return deltakontrib;
  }

  public OSHDBTimestamp getTimestap() {
    return timestap;
  }

}
