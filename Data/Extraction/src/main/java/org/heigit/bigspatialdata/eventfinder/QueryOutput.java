package org.heigit.bigspatialdata.eventfinder;

import java.util.SortedMap;

import org.heigit.bigspatialdata.oshdb.api.generic.OSHDBCombinedIndex;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;

public class QueryOutput {
	
	private SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> results;
	private String end_month;
	
	QueryOutput(SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> results, String end_month) {
		this.end_month = end_month;
		this.results = results;
	}
	
	public String get_end_month() {
		return this.end_month;
	}
	
	public SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> get_results() {
		return this.results;
	}

}
