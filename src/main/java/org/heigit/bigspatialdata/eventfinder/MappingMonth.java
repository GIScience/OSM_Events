package org.heigit.bigspatialdata.eventfinder;

import java.util.HashMap;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class MappingMonth {

  private Integer contributions; // total number of contributions during t
  private HashMap<ContributionType, Integer> types_count; // number of contributions by each type
  private HashMap<Integer, Integer> user_counts; // number of active users during t

  public MappingMonth(Integer contributions, HashMap<Integer, Integer> user_counts,
      HashMap<ContributionType, Integer> types_count) {
    this.user_counts = user_counts;
    this.contributions = contributions;
    this.types_count = types_count;
  }

  public HashMap<Integer, Integer> getUser_counts() {
    return user_counts;
  }

  public void setUser_counts(HashMap<Integer, Integer> user_counts) {
    this.user_counts = user_counts;
  }

  public Integer get_contributions() {
    return this.contributions;
  }

  public void set_contributions(Integer contributions) {
    this.contributions = contributions;
  }

  public HashMap<ContributionType, Integer> get_type_counts() {
    return this.types_count;
  }

  public void set_types_count(HashMap<ContributionType, Integer> types_count) {
    this.types_count = types_count;
  }

}
