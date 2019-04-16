package org.heigit.bigspatialdata.eventfinder;

import java.util.HashMap;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class MappingEvent extends MappingMonth {

  private final int entitiesChanged;

  private final int users;

  private final float change;

  private final float deltakontrib;

  private final OSHDBTimestamp timestap;

  private final double[] coeffs;

  private final Integer maxCont;

  private final HashMap<ContributionType, Integer> type_counts;

  private final Double pvalue;

  MappingEvent(
      OSHDBTimestamp key,
      MappingMonth value,
      int users,
      float contributions,
      float delta,
      Integer maxCont,
      double[] coeffs,
      HashMap<ContributionType, Integer> type_counts,
      int entitiesChanged,
      Double pvalue) {
    super(
        value.get_contributions(),
        value.getUser_counts(),
        value.get_type_counts(),
        value.get_edit_counts());
    this.timestap = key;
    this.users = users;
    this.deltakontrib = delta;
    this.change = contributions;
    this.coeffs = coeffs;
    this.maxCont = maxCont;
    this.type_counts = type_counts;
    this.entitiesChanged = entitiesChanged;
    this.pvalue = pvalue;
  }

  public float getChange() {
    return change;
  }

  public float getDeltakontrib() {
    return deltakontrib;
  }

  public int getEntitiesChanged() {
    return entitiesChanged;
  }

  public OSHDBTimestamp getTimestap() {
    return timestap;
  }

  public int getUsers() {
    return this.users;
  }

  public int getMaxCont() {
    return this.maxCont;
  }

  public double[] getCoeffs() {
    return this.coeffs;
  }

  public HashMap<ContributionType, Integer> getType_counts() {
    return this.type_counts;
  }

  public float get_tag_change_average() {
    //prevent division by 0
    return 1.0f * this.get_edit_counts().get_TAG() / (this.entitiesChanged + 1);
  }

  public float get_geom_change_average() {
    return 1.0f * this.get_edit_counts().get_GEOM() / (this.entitiesChanged + 1);
  }

  public Double get_pvalue() {
    return this.pvalue;
  }

}
