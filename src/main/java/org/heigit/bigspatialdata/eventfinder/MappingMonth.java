package org.heigit.bigspatialdata.eventfinder;

import java.util.HashMap;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class MappingMonth {

  private Integer contributions; // total number of contributions during t
  private HashMap<ContributionType, Integer> types_count; // number of contributions by each type
  private HashMap<Integer, Integer> user_counts; // number of active users during t
  private EditCountEnum edit_counts;
  private int users_number;
  private int max_cont;

  public MappingMonth(Integer contributions, HashMap<Integer, Integer> user_counts,
      HashMap<ContributionType, Integer> types_count, EditCountEnum edit_counts,
      int users_number, int max_cont) {
    this.user_counts = user_counts;
    this.contributions = contributions;
    this.types_count = types_count;
    this.edit_counts = edit_counts;
    this.users_number = users_number;
    this.max_cont = max_cont;
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
  
  public int[] pairwise_sum(int[] a, int[] b) {
	  int[] sum = new int[a.length];
	  for (int i=0; i<a.length; i++) {
		  sum[i] = a[i] + b[i];
	  }
	  return sum;
  }
  
  public EditCountEnum get_edit_counts() {
	  return this.edit_counts;
  }
  
  public void set_edit_counts(EditCountEnum edit_counts) {
	  this.edit_counts = edit_counts;
  }
  
  public int get_users_number() {
	  return this.users_number;
  }
  
  public void set_users_number(int users_number) {
	  this.users_number = users_number;
  }
  
  public int get_max_cont() {
	  return this.max_cont;
  }
  
  public void set_max_cont(int max_cont) {
	  this.max_cont = max_cont;
  }
}
