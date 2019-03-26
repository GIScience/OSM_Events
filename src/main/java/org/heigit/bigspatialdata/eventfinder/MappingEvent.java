package org.heigit.bigspatialdata.eventfinder;

import java.util.HashMap;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class MappingEvent extends MappingMonth {
  private final int users;
  
  private final float change;

  private final float deltakontrib;

  private final OSHDBTimestamp timestap;
  
  private final double[] coeffs;
  
  private final Integer maxCont;
  
  private final HashMap<ContributionType, Integer> type_counts;
  
  private final float tag_change_average;
  
  private final float geom_change_average;
  
  private final Double pvalue;

  MappingEvent(OSHDBTimestamp key, MappingMonth value, int users, float contributions, float delta, Integer maxCont, double[] coeffs,
    HashMap<ContributionType, Integer> type_counts, float geom_change_average, float tag_change_average, Double pvalue) {
    super(value.get_contributions(), value.getUser_counts(), value.get_type_counts(), value.get_entity_edits());
    this.timestap = key;
    this.users = users;
    this.deltakontrib = delta;
    this.change = contributions;
    this.coeffs = coeffs;
    this.maxCont = maxCont;
    this.type_counts = type_counts;
    this.geom_change_average = geom_change_average;
    this.tag_change_average = tag_change_average;
    this.pvalue = pvalue;
  }

  public float getChange() {
    return change;
  }

  public float getDeltakontrib() {
    return deltakontrib;
  }

  public OSHDBTimestamp getTimestap() {
    return timestap;
  }
  
  public int getUsers(){
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
	  return this.tag_change_average;
  }
  
  public float get_geom_change_average() {
	  return this.geom_change_average;
  }
  
  public Double get_pvalue() {
	  return this.pvalue;
  }
}
