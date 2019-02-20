package org.heigit.bigspatialdata.eventfinder;

import java.util.HashMap;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class Mapping_Event {

  private final OSHDBTimestamp t; // month of the event
  private Integer users; // number of active users during t
  private Integer contributions; // total number of contributions during t
  private Integer max_contribution; // maximal number of contributions made by one user during t
  private Float change; //change from previous time period
  private HashMap<ContributionType, Integer> types_count; // number of contributions by each type

  public Mapping_Event(OSHDBTimestamp t, Integer users, Integer contributions,
      Integer max_contribution, Float change, HashMap<ContributionType, Integer> types_count) {
    this.t = t;
    this.users = users;
    this.contributions = contributions;
    this.max_contribution = max_contribution;
    this.change = change;
    this.types_count = types_count;
  }

  public OSHDBTimestamp get_t() {
    return this.t;
  }

  public Integer get_users() {
    return this.users;
  }

  public Integer get_contributions() {
    return this.contributions;
  }

  public Integer get_max_contribution() {
    return this.max_contribution;
  }

  public Float get_change() {
    return this.change;
  }

  public HashMap<ContributionType, Integer> get_type_counts() {
    return this.types_count;
  }

  public void set_users(Integer users) {
    this.users = users;
  }

  public void set_contributions(Integer contributions) {
    this.contributions = contributions;
  }

  public void set_max_contribution(Integer max_contribution) {
    this.max_contribution = max_contribution;
  }

  public void set_change(Float change) {
    this.change = change;
  }

  public void set_types_count(HashMap<ContributionType, Integer> types_count) {
    this.types_count = types_count;
  }

}
